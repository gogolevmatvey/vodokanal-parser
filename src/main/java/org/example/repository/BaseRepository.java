package org.example.repository;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс репозитория
 * @param <T> Тип сущности
 * @param <ID> Тип идентификатора
 */
public interface BaseRepository<T, ID> {
    
    /**
     * Найти сущность по идентификатору
     * @param id идентификатор
     * @return найденная сущность или пустой Optional
     */
    Optional<T> findById(ID id);
    
    /**
     * Найти все сущности
     * @return список всех сущностей
     */
    List<T> findAll();
    
    /**
     * Сохранить сущность
     * @param entity сущность для сохранения
     * @return сохраненная сущность
     */
    T save(T entity);
    
    /**
     * Удалить сущность по идентификатору
     * @param id идентификатор
     */
    void deleteById(ID id);
}
