package com.examples.javamanual;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Scanner;

public class JDBCUpdateDemo {
  public static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
  public static final String CONNECTION_STRING =
      "jdbc:mysql://localhost:3306/web?user=root&password=root";

  public static void main(String[] args) {
    // for information purposes, may be commented
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      System.out.println(drivers.nextElement());
    }

    // Loading driver into memory (platform independent) ...
    try {
      Class.forName(DRIVER_NAME);
    } catch (ClassNotFoundException e) {
      System.out.println("Driver not found or wrong name!");
      return;
    }
    // try-with-resources. Connecting to existing database...
    try (
        Connection conn = DriverManager.getConnection(CONNECTION_STRING)
    ) {
      // for testing, may be commented.
      ResultSet rs = conn.getMetaData().getCatalogs();
      System.out.println("Catalogs of current instance:");
      while (rs.next()) {
        System.out.println(rs.getString("TABLE_CAT"));
      }
      rs.close();

      Scanner in = new Scanner(System.in);
      System.out.print("Course name: ");
      String title = in.nextLine().trim();
      System.out.print("Course duration: ");
      int length = in.nextInt();

      if (in.hasNextLine()) {
        in.nextLine();
      }

      System.out.println("Course description: ");
      String description = in.nextLine().trim();
      // Statement st = conn.createStatement(); // unsafe!
      try {
        conn.setTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED
        );
        conn.setAutoCommit(false);
        String sql =
            "INSERT INTO Courses (title, length, description)" +
                " VALUES (?, ?, ?)";
        PreparedStatement cmd = conn.prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS
        );
        cmd.setString(1, title);
        cmd.setInt(2, length);
        cmd.setString(3, description);

        if (cmd.executeUpdate() == 1) {
          System.out.println("Course has been added.");
          conn.commit();
          try (ResultSet keys = cmd.getGeneratedKeys()) {
            if (keys.next()) {
              int id = keys.getInt("id");
              System.out.printf(
                  "Course has been added. id: %d\n", id);
            }
          }
        }
      } catch (SQLException e) {
        System.out.println("Transaction error. Rolling back.");
        conn.rollback();
      }
    } catch (SQLException e) {
      System.out.println("Wrong connection: " + e.getMessage());
    } // неявный вызов conn.close();
  }
}
