package com.dmdev.jdbc.starter.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionManager
{
    /*
    private static final String PASSWORD = "postgres";
    private static final String USERNAME = "postgres";
    private static final String URL =
        "jdbc:postgresql://localhost:5432/flight_repository";
    */
    private static final String PASSWORD_KEY = "db.password";
    private static final String USERNAME_KEY = "db.username";
    private static final String URL_KEY = "db.url";

    static {
        // загружаем класс, переданный в качестве строки в оперативную память JVM
        // после java 1.8 эта память называется MetaSpace
        loadDriver();
    }

    private ConnectionManager() {}

    private static void loadDriver()
    {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // обязаны пробросить исключение, чтобы прекратить дальнейшее выполнение
            throw new RuntimeException(e);
        }
    }

    public static Connection open()
    {
        try {
            //return DriverManager.getConnection(URL, USERNAME, PASSWORD);
            return DriverManager.getConnection(
                PropertiesUtil.get(URL_KEY),
                PropertiesUtil.get(USERNAME_KEY),
                PropertiesUtil.get(PASSWORD_KEY)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
