package org.example.service.search;

import org.example.model.domain.Apartment;
import org.example.repository.ApartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с квартирами
 */
@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    public ApartmentService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    /**
     * Найти квартиры по дому и номеру (автокомплит)
     */
    public List<Apartment> searchByHouseAndNumber(Long houseId, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return apartmentRepository.findByHouseId(houseId).stream().limit(limit).toList();
        }
        
        if (query.length() < 1) {
            return apartmentRepository.findByHouseId(houseId).stream().limit(limit).toList();
        }
        
        return apartmentRepository.findByHouseIdAndNumberContaining(houseId, query).stream()
            .limit(limit)
            .toList();
    }

    /**
     * Найти квартиру по ID
     */
    public Apartment findById(Long id) {
        return apartmentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Квартира не найдена: " + id));
    }
}
