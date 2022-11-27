package com.examples.sedykh;

import java.sql.*;
import java.util.Enumeration;

public class JDBCDemo2
{
    public static final String DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    public static final String CONN_STRING =
        "jdbc:mysql://localhost:3306/?user=root&password=root";
    public static void main(String[] args)
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            System.out.println(drivers.nextElement());
        }
        System.out.println("*********************************");

        // 1. Загружаем драйвер в память, не создавая объект его типа
        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found or wrong driver name!");
            return;
        }

        // 2. устанавливаем соединение с БД
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(CONN_STRING);
        } catch (SQLException e) {
            System.out.println("Cannot open connection to DB!");
            System.out.println(e.getMessage());
            return;
        }

        // 3. подача запросов на сервер
        try {
            ResultSet rs = conn.getMetaData().getCatalogs();
            System.out.println("Databases list:");
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_CAT"));
            }
            System.out.println("*********************************");

            Statement st = conn.createStatement();
            /*
            st.executeUpdate("CREATE DATABASE db");
            st.executeUpdate("USE db");
            st.executeUpdate(
                "CREATE TABLE persons" +
                    "(" +
                        "id INT(8) NOT NULL AUTO_INCREMENT, " +
                        "name VARCHAR(32) NOT NULL," +
                        "age INT(3)," +
                        "PRIMARY KEY(id)" +
                    ")");
             */
            st.executeUpdate("USE db");
            /*
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Иван', 25)");
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Антон', 15)");
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Михаил', 15)");
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Сергей', 10)");
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Ян', 17)");
            st.executeUpdate(
                "INSERT INTO persons (name, age) VALUES ('Тимофей', 32)");
             */
            System.out.println("Structure of the table \"persons\":");

            rs = st.executeQuery("DESC persons");
            while (rs.next()) {
                int i = 1;
                System.out.printf("%-6s %-12s %-6s %-6s %-6s %-6s\n",
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6));
            }
            System.out.println("*********************************");

            System.out.println("Content of the table \"persons\":");
            rs = st.executeQuery(
                "SELECT * FROM persons WHERE age = 15 ORDER BY name");
            while (rs.next()) {
                System.out.printf("%s %s %s\n",
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("age"));
            }
            System.out.println("*********************************");

            System.out.println("Example of prepared statement");
            String q1 = "SELECT * FROM persons WHERE name LIKE ? AND age = ?";
            PreparedStatement ps = conn.prepareStatement(q1);
            ps.setString(1, "А%");
            ps.setInt(2, 15);
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                System.out.printf("%s %s %s\n",
                    rs2.getString("id"),
                    rs2.getString("name"),
                    rs2.getString("age"));
            }
            System.out.println("*********************************");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }


    }
}
