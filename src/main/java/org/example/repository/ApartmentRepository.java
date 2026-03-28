package org.example.repository;

import org.example.parser.model.Apartment;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с квартирами
 */
public interface ApartmentRepository extends BaseRepository<Apartment, Long> {
    
    /**
     * Найти квартиры по идентификатору дома
     * @param houseId идентификатор дома
     * @return список квартир
     */
    List<Apartment> findByHouseId(Long houseId);
    
    /**
     * Найти квартиры по идентификатору дома и части номера
     * @param houseId идентификатор дома
     * @param number часть номера квартиры
     * @return список квартир
     */
    List<Apartment> findByHouseIdAndNumberContaining(Long houseId, String number);
    
    /**
     * Найти квартиру по идентификатору дома и номеру
     * @param houseId идентификатор дома
     * @param number номер квартиры
     * @return найденная квартира или пустой Optional
     */
    Optional<Apartment> findByHouseIdAndNumber(Long houseId, String number);
}
