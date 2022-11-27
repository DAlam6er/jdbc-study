package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;

public class BlobRunner
{
    public static void main(String[] args) throws SQLException, IOException
    {
        // java.sql.SQLFeatureNotSupportedException
        //saveImage();

        //saveImageInPostgreSQL();
        getImage();
    }

    private static void saveImageInPostgreSQL() throws SQLException, IOException
    {
        var sql = """
            UPDATE aircraft
            SET image = ?
            WHERE id = 1
            """;
        try (var connection = ConnectionManager.open();
             var preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.setBytes(1, Files.readAllBytes(
                Path.of("..", "jdbc", "resources", "boeing777.jpg")));
            preparedStatement.executeUpdate();
        }
    }


    private static void getImage() throws SQLException, IOException
    {
        var sql = """
            SELECT image
            FROM aircraft
            WHERE id = ?
            """;
        try (var connection = ConnectionManager.open();
             var preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.setInt(1, 1);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                var image = resultSet.getBytes("image");
                Files.write(Path.of("..", "jdbc", "resources", "boeing777_new.jpg"),
                    image, StandardOpenOption.CREATE);
            }
        }
    }

    // Сохранение изображения в СУБД, поддерживающей тип данных Blob
    private static void saveImage() throws SQLException, IOException
    {
        var sql = """
            UPDATE aircraft
            SET image = ?
            WHERE id = 1
            """;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = ConnectionManager.open();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(sql);
            var blob = connection.createBlob();
            blob.setBytes(1, Files.readAllBytes(
                Path.of("..", "jdbc", "resources", "boeing777.jpg")));

            preparedStatement.setBlob(1, blob);
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            if (connection != null) {
                connection.rollback();
            }
            throw ex;
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }
}
