package org.example.repository;

import org.example.parser.model.House;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с домами
 */
public interface HouseRepository extends BaseRepository<House, Long> {

    /**
     * Найти дома по идентификатору улицы
     * @param streetId идентификатор улицы
     * @return список домов
     */
    List<House> findByStreetId(Long streetId);

    /**
     * Найти дома по идентификатору улицы и части номера
     * @param streetId идентификатор улицы
     * @param number часть номера дома
     * @return список домов
     */
    List<House> findByStreetIdAndNumberContaining(Long streetId, String number);
    
    /**
     * Найти дом по идентификатору улицы и номеру
     * @param streetId идентификатор улицы
     * @param number номер дома
     * @return найденный дом или пустой Optional
     */
    Optional<House> findByStreetIdAndNumber(Long streetId, String number);
}
