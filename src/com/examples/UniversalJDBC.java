package com.examples;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
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

public class UniversalJDBC {
  public static void main(String[] args) {
    try (Connection conn = establishConnection()) {
      while (true) {
        System.out.println("********************************************");
        System.out.println(
            "1 — execute the SQL Statement (INSERT, UPDATE or DELETE);\n" +
                "2 — execute the SQL Statement to retrieve data  (SELECT);\n" +
                "3 — get catalog names available in this database;\n" +
                "4 — exit");
        System.out.println("********************************************");
        System.out.print("Choose option to continue: ");
        String choice = new Scanner(System.in).nextLine();
        switch (choice) {
          case "1":
            Statement st = conn.createStatement();
            st.executeUpdate(new Scanner(System.in).nextLine());
            break;
          case "2":
            System.out.println("Input your query:");
            printResultSet(
                conn, new Scanner(System.in).nextLine());
            break;
          case "3":
            getDBList(conn);
            break;
          default:
            return;
        }
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public static Connection establishConnection()
      throws SQLException, NoSuchElementException {
    Driver driver = null;
    try {
      driver = loadDriver();
    } catch (ClassNotFoundException | NullPointerException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }


    System.out.println("Input database name to establish connection with " +
        "or use this field blank to connect to default instance: ");
    String dbName = new Scanner(System.in).nextLine();

    UserPassword uspass = new UserPassword();
    System.out.print("Input userName: ");
    uspass.setUserName(new Scanner(System.in).nextLine());
    System.out.print("Input password: ");
    uspass.setUserPassword(new Scanner(System.in).nextLine());

    final String driverName = driver.getClass().getName();

    Optional<DBMS> dbmsOpt = Stream.of(DBMS.values())
        .filter(dbms -> driverName.equals(dbms.getDriverName()))
        .findFirst();

    try {
      DBMS dbms = dbmsOpt.orElseThrow(NoSuchElementException :: new);
      Connection connection;
      if (!"".equals(dbName)) {
        connection = DriverManager.getConnection(
            dbms.getConnection(dbName),
            uspass.getUserName(),
            uspass.getUserPassword());
        System.out.printf(
            "Connection with %s database %s " +
                "established successfully.\n",
            dbms.name(), dbName);
      } else {
        connection = DriverManager.getConnection(
            dbms.getConnection(),
            uspass.getUserName(),
            uspass.getUserPassword());
        System.out.printf(
            "Connection with %s server established successfully.\n",
            dbms.name());
      }
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

  public static void printResultSet(Connection conn, String SQLString)
      throws SQLException {
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery(SQLString);
    ResultSetMetaData rsmd = rs.getMetaData();
    int columnsNumber = rsmd.getColumnCount();

    String columnValue;
    while (rs.next()) {
      for (int i = 1; i <= columnsNumber; i++) {
        if (i > 1) System.out.println(", ");
        columnValue = rs.getString(i);
        System.out.println(columnValue + "\t" + rsmd.getColumnName(i));
      }
    }
  }

  public static void getDBList(Connection conn) throws SQLException {
    System.out.println("Catalog names available in this database:");
    ResultSet rs = conn.getMetaData().getCatalogs();
    while (rs.next()) {
      System.out.println(
          "\t" + rs.getString("TABLE_CAT"));
    }
  }

  private static Driver loadDriver()
      throws ClassNotFoundException, NullPointerException {
    HashMap<Integer, Driver> driversMap = new HashMap<>();
    int i = 0;

    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      driversMap.put(++i, drivers.nextElement());
    }

    System.out.println("Select driver by number to use it with JDBC:");
    System.out.println("********************************************");
    for (Map.Entry<Integer, Driver> driversEntry : driversMap.entrySet()) {
      System.out.printf("%s\t%s\n",
          driversEntry.getKey(),
          driversEntry.getValue().getClass().getName());
    }
    System.out.println("********************************************");
    System.out.print("Your choice: ");
    try {
      Driver driver = driversMap.get(new Scanner(System.in).nextInt());
      Class.forName(driver.getClass().getName());
      System.out.println("Driver loaded successfully");
      return driver;
    } catch (ClassNotFoundException e) {
      throw new ClassNotFoundException(
          "Driver not found or wrong driver name!");
    } catch (NullPointerException e) {
      throw new NullPointerException(
          "Driver matching selection not found!");
    }
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
