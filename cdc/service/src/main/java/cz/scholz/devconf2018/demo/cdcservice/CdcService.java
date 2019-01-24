package cz.scholz.devconf2018.demo.cdcservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdcService extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(CdcService.class.getName());

    private final CdcServiceConfig verticleConfig;
    private SQLConnection sql;

    public CdcService(CdcServiceConfig verticleConfig) throws Exception {
        log.info("Creating Service");
        this.verticleConfig = verticleConfig;
    }

    /*
    Start the verticle
     */
    @Override
    public void start(Future<Void> start) {
        log.info("Starting Service");

        // SQL Database
        JsonObject mysqlSQLClientConfig = new JsonObject()
                .put("host", verticleConfig.getDbHost())
                .put("port", Integer.parseInt(verticleConfig.getDbPort()))
                .put("database", verticleConfig.getDbName())
                .put("username", verticleConfig.getDbUsername())
                .put("password", verticleConfig.getDbPassword())
                .put("maxConnectionRetries", -1);
        SQLClient sqlCLient = MySQLClient.createShared(vertx, mysqlSQLClientConfig);

        sqlCLient.getConnection(res -> {
            if (res.succeeded()) {
                sql = res.result();


            } else {
                log.error("Failed to connect to DB", res.cause());
                start.fail("Failed to connect to DB");
            }
        });

        // REST API

        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/addressbook/*").handler(BodyHandler.create());
        router.post("/addressbook").handler(req -> {
            log.info("Received POST /addressbook request");

            JsonObject request = req.getBodyAsJson();
            String firstName = request.getString("first_name", null);
            String lastName = request.getString("last_name", null);
            String email = request.getString("email", null);

            String query = "INSERT INTO customers (first_name, last_name, email) VALUES ('" + firstName + "', '" + lastName + "', '" + email + "')";

            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    Lock lock = locker.result();
                    log.info("Creating address " + request);

                    sql.update(query, res2 -> {
                        if (res2.succeeded()) {
                            UpdateResult result = res2.result();
                            log.info("Updated {} rows. New ID {}", result.getUpdated(), result.getKeys());

                            req.response()
                                    .setStatusCode(201)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(new JsonObject().put("id", result.getKeys().getInteger(0)).put("first_name", firstName).put("last_name", lastName).put("email", email).encodePrettily());
                        } else {
                            log.error("Failed to add address");

                            req.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(new JsonObject().put("error", "Failed to add address: " + res2.cause().getMessage()).encodePrettily());
                        }

                        lock.release();
                    });
                } else {
                    log.error("Failed to acquire lock", locker.cause());

                    req.response()
                            .setStatusCode(500)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(new JsonObject().put("error", "Failed to acquire lock: " + locker.cause().getMessage()).encodePrettily());
                }
            });
        });

        router.put("/addressbook/:id").handler(req -> {
            log.info("Received PUT /addressbook request");

            String id = req.request().getParam("id");

            JsonObject request = req.getBodyAsJson();
            String firstName = request.getString("first_name", null);
            String lastName = request.getString("last_name", null);
            String email = request.getString("email", null);

            String query = "UPDATE customers SET first_name='" + firstName + "', last_name='" + lastName + "', email='" + email + "' WHERE id=" + id;

            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    Lock lock = locker.result();
                    log.info("Updating address " + request);

                    sql.update(query, res2 -> {
                        if (res2.succeeded()) {
                            UpdateResult result = res2.result();
                            log.info("Updated {} rows. New IDs {}", result.getUpdated(), result.getKeys());

                            req.response()
                                    .setStatusCode(200)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(new JsonObject().put("id", id).put("first_name", firstName).put("last_name", lastName).put("email", email).encodePrettily());
                        } else {
                            log.error("Failed to add address");

                            req.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(new JsonObject().put("error", "Failed to change address: " + res2.cause().getMessage()).encodePrettily());
                        }

                        lock.release();
                    });
                } else {
                    log.error("Failed to acquire lock", locker.cause());

                    req.response()
                            .setStatusCode(500)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(new JsonObject().put("error", "Failed to acquire lock: " + locker.cause().getMessage()).encodePrettily());
                }
            });
        });

        router.delete("/addressbook/:id").handler(req -> {
            log.info("Received DELETE /addressbook request");

            String id = req.request().getParam("id");

            String query = "DELETE FROM customers WHERE id=" + id;

            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    Lock lock = locker.result();
                    log.info("Deleting address " + id);

                    sql.update(query, res2 -> {
                        if (res2.succeeded()) {
                            UpdateResult result = res2.result();
                            log.info("Deleted {} rows. New IDs {}", result.getUpdated(), result.getKeys());

                            req.response()
                                    .setStatusCode(200)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end();
                        } else {
                            log.error("Failed to add address");

                            req.response()
                                    .setStatusCode(400)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(new JsonObject().put("error", "Failed to delete address: " + res2.cause().getMessage()).encodePrettily());
                        }

                        lock.release();
                    });
                } else {
                    log.error("Failed to acquire lock", locker.cause());

                    req.response()
                            .setStatusCode(500)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(new JsonObject().put("error", "Failed to acquire lock: " + locker.cause().getMessage()).encodePrettily());
                }
            });
        });

        router.get("/adressbook").handler(routingContext -> {
            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    log.debug("I have a lock");
                    Lock lock = locker.result();

                    sql.query("SELECT * FROM customers", res2 -> {
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

        router.get("/adressbook/:id").handler(routingContext -> {
            vertx.sharedData().getLockWithTimeout("sqlLock", 30000, locker -> {
                if (locker.succeeded()) {
                    log.debug("I have a lock");
                    Lock lock = locker.result();

                    String id = routingContext.request().getParam("id");

                    sql.query("SELECT * FROM customers WHERE id=" + id, res2 -> {
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

        router.route("/*").handler(StaticHandler.create());

        server.requestHandler(router).listen(8080);
    }

    /*
    Stop the verticle
     */
    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        log.info("Stopping the consumer.");
        sql.close(res2 -> {
            stopFuture.complete();
        });
    }
}
