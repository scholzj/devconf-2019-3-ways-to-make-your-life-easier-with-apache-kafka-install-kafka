package cz.scholz.devconf2018.demo.replayloader;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.kafka.client.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayLoader extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ReplayLoader.class.getName());

    private final ReplayLoaderConfig verticleConfig;
    private KafkaConsumer<String, String> consumer;
    private SQLConnection sql;
    private final boolean commit;

    public ReplayLoader(ReplayLoaderConfig verticleConfig) throws Exception {
        log.info("Creating Replay Loader");
        this.verticleConfig = verticleConfig;
        commit = !Boolean.parseBoolean(verticleConfig.getEnableAutoCommit());
    }

    /*
    Start the verticle
     */
    @Override
    public void start(Future<Void> start) {
        log.info("Starting Replay Loader");

        // Kafka

        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", verticleConfig.getBootstrapServers());
        config.put("group.id", verticleConfig.getGroupId());
        config.put("auto.offset.reset", verticleConfig.getAutoOffsetReset());
        config.put("enable.auto.commit", verticleConfig.getEnableAutoCommit());
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = KafkaConsumer.create(vertx, config, String.class, String.class);

        consumer.handler(res -> {
            log.info("Received message (topic: {}, partition: {}, offset: {}) with key {}: {}", res.topic(), res.partition(), res.offset(), res.key(), res.value());

            JsonObject msg = new JsonObject(res.value());
            String symbol = msg.getString("symbol");
            Integer amount = msg.getInteger("amount");

            String query;

            if (msg.getString("type").equals("B")) {
                query = "INSERT INTO shares VALUES ('" + symbol + "', " + amount + ") ON CONFLICT (symbol) DO UPDATE SET amount=EXCLUDED.amount+" + amount;
            } else {
                query = "INSERT INTO shares VALUES ('" + symbol + "', -" + amount + ") ON CONFLICT (symbol) DO UPDATE SET amount=EXCLUDED.amount-" + amount;
            }

            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    Lock lock = locker.result();

                    sql.update(query, res2 -> {
                        if (res2.succeeded()) {
                            UpdateResult result = res2.result();
                            log.info("Updated {} rows", result.getUpdated());

                            if (commit) {
                                consumer.commit();
                            }
                        } else {
                            log.error("Failed to load record {} into database", msg, res2.cause());
                        }

                        lock.release();
                    });
                } else {
                    log.error("Failed to acquire lock", locker.cause());
                }
            });
        });

        consumer.exceptionHandler(res -> {
            log.error("Received exception", res);
        });

        // SQL Database

        JsonObject postgreSQLClientConfig = new JsonObject()
                .put("host", verticleConfig.getDbHost())
                .put("port", Integer.parseInt(verticleConfig.getDbPort()))
                .put("database", verticleConfig.getDbName())
                .put("username", verticleConfig.getDbUsername())
                .put("password", verticleConfig.getDbPassword())
                .put("maxConnectionRetries", -1);
        SQLClient sqlCLient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);

        sqlCLient.getConnection(res -> {
            if (res.succeeded()) {
                sql = res.result();

                consumer.subscribe(verticleConfig.getTopic(), res2 -> {
                    if (res2.succeeded()) {
                        log.info("Subscribed to topic {}", verticleConfig.getTopic());
                        start.complete();
                    }
                    else {
                        log.error("Failed to subscribe to topic {}", verticleConfig.getTopic(), res2.cause());
                        start.fail("Failed to subscribe to topic " + verticleConfig.getTopic());
                    }
                });
            } else {
                log.error("Failed to connect to DB", res.cause());
                start.fail("Failed to connect to DB");
            }
        });

        // REST API

        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(routingContext -> {
            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    log.debug("I have a lock");
                    Lock lock = locker.result();

                    sql.query("SELECT * FROM shares", res2 -> {
                        if (res2.succeeded()) {
                            log.debug("Query succeeded");

                            String output = new JsonArray(res2.result().getRows()).encodePrettily();
                            log.debug("Got output: {}", output);

                            HttpServerResponse response = routingContext.response();
                            response.putHeader("content-type", "application/json");
                            response.end(output);
                        } else {
                            log.error("Failed to query data from database", res2.cause());
                            HttpServerResponse response = routingContext.response();
                            response.setStatusCode(500);
                            response.putHeader("content-type", "plain/text");
                            response.end("Failed to query data from database");
                        }

                        log.debug("Releasing lock");
                        lock.release();
                    });
                } else {
                    log.error("Failed to acquire lock", locker.cause());
                }
            });
        });

        server.requestHandler(router).listen(8080);
    }

    /*
    Stop the verticle
     */
    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        log.info("Stopping the consumer.");
        consumer.endHandler(res -> {
            sql.close(res2 -> {
                stopFuture.complete();
            });
        });
    }
}
