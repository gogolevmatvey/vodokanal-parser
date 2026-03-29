package org.example.repository;

import org.example.model.domain.Street;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с улицами
 */
public interface StreetRepository extends BaseRepository<Street, Long> {

    /**
     * Найти улицы по идентификатору населенного пункта
     * @param localityId идентификатор населенного пункта
     * @return список улиц
     */
    List<Street> findByLocalityId(Long localityId);

    /**
     * Найти улицы по идентификатору населенного пункта и части названия
     * @param localityId идентификатор населенного пункта
     * @param name часть названия улицы
     * @return список улиц
     */
    List<Street> findByLocalityIdAndNameContaining(Long localityId, String name);
    
    /**
     * Найти улицу по идентификатору населенного пункта и названию
     * @param localityId идентификатор населенного пункта
     * @param name название улицы
     * @return найденная улица или пустой Optional
     */
    Optional<Street> findByLocalityIdAndName(Long localityId, String name);
}
