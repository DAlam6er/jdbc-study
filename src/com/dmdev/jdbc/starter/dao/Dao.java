package com.dmdev.jdbc.starter.dao;

import java.util.List;
import java.util.Optional;

public interface Dao<K, E> {
  E save(E entity);

  Optional<E> findById(K id);

  List<E> findAll();

  void update(E entity);

  boolean delete(K id);
}
