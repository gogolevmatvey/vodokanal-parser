package org.example.repository;

import org.example.parser.model.Locality;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с населенными пунктами
 */
public interface LocalityRepository extends BaseRepository<Locality, Long> {
    
    /**
     * Найти населенные пункты по части названия
     * @param name часть названия
     * @return список найденных населенных пунктов
     */
    List<Locality> findByNameContaining(String name);
    
    /**
     * Найти населенный пункт по названию
     * @param name название
     * @return найденный населенный пункт или пустой Optional
     */
    Optional<Locality> findByName(String name);
}
