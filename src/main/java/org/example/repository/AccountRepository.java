package org.example.repository;

import org.example.parser.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с лицевыми счетами
 */
public interface AccountRepository extends BaseRepository<Account, Long> {
    
    /**
     * Найти лицевой счет по номеру
     * @param accountNumber номер лицевого счета
     * @return найденный счет или пустой Optional
     */
    Optional<Account> findByAccountNumber(String accountNumber);
    
    /**
     * Найти лицевые счета по идентификатору квартиры
     * @param apartmentId идентификатор квартиры
     * @return список лицевых счетов
     */
    List<Account> findByApartmentId(Long apartmentId);
    
    /**
     * Найти лицевой счет по идентификатору квартиры и номеру
     * @param apartmentId идентификатор квартиры
     * @param accountNumber номер лицевого счета
     * @return найденный счет или пустой Optional
     */
    Optional<Account> findByApartmentIdAndAccountNumber(Long apartmentId, String accountNumber);
}
