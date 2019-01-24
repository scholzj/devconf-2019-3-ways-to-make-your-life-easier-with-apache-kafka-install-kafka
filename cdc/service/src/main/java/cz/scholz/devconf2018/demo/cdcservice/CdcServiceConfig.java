package cz.scholz.devconf2018.demo.cdcservice;

public class CdcServiceConfig {
    // Database
    private final String dbName = "inventory";
    private final String dbHost = "mysql";
    private final String dbPort = "3306";
    private final String dbUsername = "mysqluser";
    private final String dbPassword = "mysqlpw";

    public CdcServiceConfig() {
    }

    public static CdcServiceConfig fromEnv() {
        return new CdcServiceConfig();
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbPort() {
        return dbPort;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }
}
