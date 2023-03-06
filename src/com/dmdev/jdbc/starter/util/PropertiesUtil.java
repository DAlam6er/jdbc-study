package com.dmdev.jdbc.starter.util;

import java.io.IOException;
import java.util.Properties;

public final class PropertiesUtil {
  // это обычный ассоциативный массив
  // старая коллекция, больше не используется
  // public class Properties extends HashTable<Object, Object>
  // её прямой аналог - ConcurrentHashMap
  private static final Properties PROPERTIES = new Properties();

  static {
    loadProperties();
  }

  private PropertiesUtil() {}

  private static void loadProperties() {
    try (var inputStream = PropertiesUtil.class.getClassLoader()
        .getResourceAsStream("application.properties")) {
      PROPERTIES.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String get(String key) {
    return PROPERTIES.getProperty(key);
  }
}
