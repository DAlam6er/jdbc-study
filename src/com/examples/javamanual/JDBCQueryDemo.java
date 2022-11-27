package com.examples.javamanual;

import java.sql.*;
import java.util.Scanner;

public class JDBCQueryDemo
{
    public static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    public static final String CONNECTION_STRING =
        "jdbc:mysql://localhost:3306/web?user=root&password=root";

    public static void main(String[] args)
    {
        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found or wrong name!");
        }

        try (
            Connection conn = DriverManager.getConnection(CONNECTION_STRING)
        ) {
            System.out.print("Searching:");
            Scanner in = new Scanner(System.in);
            String search = in.nextLine().trim();
            String sql =
                "SELECT title, length FROM courses WHERE title LIKE ? " +
                "ORDER BY title";
            PreparedStatement cmd = conn.prepareStatement(sql);
            cmd.setString(1, "%" + search + "%");
            ResultSet rs = cmd.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                int length = rs.getInt("length");

                System.out.printf("%-30s : %d\n", title, length);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
