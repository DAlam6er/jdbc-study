package com.personsDB.sedykh;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

enum DBMS {
  Oracle() {
    // jdbc:oracle:<drivertype>:@//<host>:<portNumber>/<service_name>
    public static final Integer PORT = 1521;
    public static final String DRIVER_NAME = "oracle.jdbc.driver.OracleDriver";

    @Override
    public String getDriverName() {
      return DRIVER_NAME;
    }

    @Override
    public String getConnection(String serviceName) {
      return String.format("jdbc:oracle:thin:@//%s:%d/%s",
          ServerGlobalData.HOST_NAME, PORT, serviceName);
    }

    // jdbc:oracle:<drivertype>:@<host>:<portNumber>:<service_name>
    public String getSIDConnection(String SID) {
      return String.format("jdbc:oracle:thin:@%s:%d:%s",
          ServerGlobalData.HOST_NAME, PORT, SID);
    }

    @Override
    public String getConnection() {
      return getSIDConnection("ORCL");
    }
  },

  MSSQL() {
    // jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
    public static final Integer PORT = 1433;
    public static final String DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    @Override
    public String getDriverName() {
      return DRIVER_NAME;
    }

    @Override
    public String getConnection(String dbName) {
      return String.format(
          "jdbc:sqlserver://%s:%d;databaseName=%s;" +
              "encrypt=true;trustServerCertificate=true",
          ServerGlobalData.HOST_NAME, PORT, dbName);
    }

    @Override
    public String getConnection() {
      return String.format(
          "jdbc:sqlserver://%s:%d;" +
              "encrypt=true;trustServerCertificate=true",
          ServerGlobalData.HOST_NAME, PORT);
    }
  },

  PostgreSQL() {
    // jdbc:postgresql://host:port/database
    public static final Integer PORT = 5432;
    public static final String DRIVER_NAME = "org.postgresql.Driver";

    @Override
    public String getDriverName() {
      return DRIVER_NAME;
    }

    @Override
    public String getConnection(String dbName) {
      return String.format("jdbc:postgresql://%s:%d/%s",
          ServerGlobalData.HOST_NAME, PORT, dbName);
    }

    @Override
    public String getConnection() {
      return getConnection("");
    }
  },

  MySQL() {
    // protocol//[hosts][/database][?properties]
    public static final Integer PORT = 3306;
    public static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";

    @Override
    public String getDriverName() {
      return DRIVER_NAME;
    }

    @Override
    public String getConnection(String dbName) {
      return String.format("jdbc:mysql://%s:%d/%s",
          ServerGlobalData.HOST_NAME, PORT, dbName);
    }

    @Override
    public String getConnection() {
      return getConnection("");
    }
  },

  Derby() {
    // jdbc:derby:[subsubprotocol:][databaseName][;attribute=value]*
    public static final Integer PORT = 1527;
    public static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";

    @Override
    public String getDriverName() {
      return DRIVER_NAME;
    }

    @Override
    public String getConnection(String dbName) {

      return String.format("jdbc:derby://%s:%d/%s",
          ServerGlobalData.HOST_NAME, PORT, dbName);
    }

    @Override
    public String getConnection() {
      return getConnection("");
    }
  };

  public abstract String getConnection();

  public abstract String getDriverName();

  public abstract String getConnection(String dbName);
}

class ServerGlobalData {
  public static final String HOST_NAME = "localhost";
}

public class JDBCModel {
  public static Connection establishConnection(
      Driver driver, String userName, char[] password)
      throws SQLException, NoSuchElementException, ClassNotFoundException {
    JDBCModel.UserPassword uspass = new JDBCModel.UserPassword();
    uspass.setUserName(userName);
    uspass.setUserPassword(String.valueOf(password));

    final String driverName = driver.getClass().getName();

    Optional<DBMS> dbmsOpt = Stream.of(DBMS.values())
        .filter(dbms -> driverName.equals(dbms.getDriverName()))
        .findFirst();

    try {
      DBMS dbms = dbmsOpt.orElseThrow(NoSuchElementException :: new);
      Connection connection;
      connection = DriverManager.getConnection(
          dbms.getConnection(),
          uspass.getUserName(),
          uspass.getUserPassword());
      System.out.printf(
          "Connection with %s server established successfully.\n",
          dbms.name());
      return connection;
    } catch (SQLException e) {
      throw new SQLException(
          "Cannot open connection to DB:\n" + e.getMessage());
    } catch (NoSuchElementException ex) {
      throw new NoSuchElementException(
          "Connection string not found for loaded driver: "
              + driverName);
    }
  }

  public static Driver loadDriver(String dbmsName)
      throws ClassNotFoundException, IllegalArgumentException {
    HashMap<String, Driver> driversMap = new HashMap<>();

    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      driversMap.put(driver.getClass().getName(), driver);
    }

    try {
      Driver driver =
          driversMap.get(DBMS.valueOf(dbmsName).getDriverName());
      Class.forName(driver.getClass().getName());
      return driver;
    } catch (ClassNotFoundException e) {
      throw new ClassNotFoundException(
          "Driver not found or wrong driver name!");
    } catch (NullPointerException e) {
      throw new IllegalArgumentException(
          "Connector for selected DBMS is not installed!");
    }
  }

  public static void uploadData(Connection conn, String dbName, String... data)
      throws SQLException {
    selectDB(conn, dbName);
    try {
      String sqlInsertExpr = "INSERT INTO persons (name, age) VALUES (?, ?);";
      PreparedStatement ps = conn.prepareStatement(sqlInsertExpr);
      ps.setString(1, data[0]);
      ps.setInt(2, Integer.parseInt(data[1]));
      ps.executeUpdate();
    } catch (SQLException ex) {
      throw new SQLException(
          "Record uploading failed:\n" + ex.getMessage());
    }
  }

  private static void selectDB(Connection conn, String dbName) throws SQLException {
    try {
      Statement st = conn.createStatement();
      st.executeUpdate(String.format("USE %s;", dbName));
    } catch (SQLException ex) {
      throw new SQLException(
          "Wrong database name:\n" + ex.getMessage());
    }
  }

  public static ResultSet showAllData(Connection conn, String dbName)
      throws SQLException {
    selectDB(conn, dbName);
    Statement st = conn.createStatement();
    return st.executeQuery("SELECT * FROM persons;");
  }

  public static ResultSet sortByName(Connection conn, String dbName)
      throws SQLException {
    selectDB(conn, dbName);
    Statement st = conn.createStatement();
    return st.executeQuery("SELECT * FROM persons ORDER BY name;");
  }

  public static ResultSet showAtAge(Connection conn, String dbName, String age)
      throws SQLException {
    selectDB(conn, dbName);
    String sqlSelectExpr = "SELECT * FROM persons WHERE age = ?;";
    PreparedStatement ps = conn.prepareStatement(sqlSelectExpr);
    ps.setInt(1, Integer.parseInt(age));
    return ps.executeQuery();
  }

  private static class UserPassword {
    private String userName;
    private String userPassword;

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getUserPassword() {
      return userPassword;
    }

    public void setUserPassword(String userPassword) {
      this.userPassword = userPassword;
    }
  }
}
