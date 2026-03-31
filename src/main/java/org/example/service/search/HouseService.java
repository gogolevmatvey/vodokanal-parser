package org.example.service.search;

import org.example.model.domain.House;
import org.example.repository.HouseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с домами
 */
@Service
public class HouseService {

    private final HouseRepository houseRepository;

    public HouseService(HouseRepository houseRepository) {
        this.houseRepository = houseRepository;
    }

    /**
     * Найти дома по улице и номеру (автокомплит)
     */
    public List<House> searchByStreetAndNumber(Long streetId, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return houseRepository.findByStreetId(streetId).stream().limit(limit).toList();
        }
        
        if (query.length() < 1) {
            return houseRepository.findByStreetId(streetId).stream().limit(limit).toList();
        }
        
        return houseRepository.findByStreetIdAndNumberContaining(streetId, query).stream()
            .limit(limit)
            .toList();
    }

    /**
     * Найти дом по ID
     */
    public House findById(Long id) {
        return houseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Дом не найден: " + id));
    }
}
