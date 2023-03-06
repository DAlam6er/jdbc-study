package com.myPhotoApp.sedykh;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Photos {
  public static final String DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  public static final String URL = "jdbc:sqlserver://localhost:1433;" +
      "database=AdventureWorks2019;encrypt=true;trustServerCertificate=true";

  static {
    try {
      Class.forName(DRIVER_NAME);
    } catch (ClassNotFoundException e) {
      System.err.println(e.getMessage());
    }
  }

  public static void putPhoto(String name, String path) {
    try (Connection conn = DriverManager.getConnection(URL, "sa", "root")) {
      File file = new File(path);
      if (file.exists()) {
        long size = file.length();
        BufferedInputStream bis =
            new BufferedInputStream(new FileInputStream(file));
        String sqlQuery =
            "INSERT INTO Production.ProductPhoto " +
                "(LargePhotoFileName, LargePhoto) VALUES (?, ?);";
        PreparedStatement ps = conn.prepareStatement(sqlQuery);
        ps.setString(1, name);
        ps.setBinaryStream(2, bis, size);
        ps.executeUpdate();
        System.out.println("Photo was successfully added");
      }
    } catch (SQLException ex) {
      System.err.println(
          "Cannot open connection to DB:\n" + ex.getMessage());
    } catch (FileNotFoundException ex) {
      System.err.println("File not found ");
    }
  }

  public static String[] getPhotos() {
    ArrayList<String> list = null;

    try (Connection con = DriverManager.getConnection(URL, "sa", "root")) {
      String sqlQuery =
          "SELECT ProductPhotoID, LargePhotoFileName " +
              "FROM Production.ProductPhoto;";
      Statement st = con.createStatement();
      ResultSet rs = st.executeQuery(sqlQuery);
      list = new ArrayList<>();
      while (rs.next()) {
        list.add(rs.getString(1) + " " + rs.getString(2));
      }
      rs.close();
      st.close();
    } catch (SQLException ex) {
      System.err.println(ex.getMessage());
    }

    return (list != null) ? list.toArray(new String[]{}) : null;
  }

  public static InputStream getPhoto(int id) {
    InputStream in = null;
    try (Connection conn = DriverManager.getConnection(URL, "sa", "root")) {
      String sqlQuery = "SELECT LargePhoto FROM Production.ProductPhoto " +
          "WHERE ProductPhotoID = ?";
      PreparedStatement ps = conn.prepareStatement(sqlQuery);
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      rs.next();
      in = rs.getBinaryStream(1);
    } catch (SQLException ex) {
      System.err.println(ex.getMessage());
    }
    return in;
  }
}
