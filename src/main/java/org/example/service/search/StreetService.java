package org.example.service.search;

import org.example.model.domain.Street;
import org.example.repository.StreetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с улицами
 */
@Service
public class StreetService {

    private final StreetRepository streetRepository;

    public StreetService(StreetRepository streetRepository) {
        this.streetRepository = streetRepository;
    }

    /**
     * Найти улицы по населенному пункту и названию (автокомплит)
     */
    public List<Street> searchByLocalityAndName(Long localityId, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return streetRepository.findByLocalityId(localityId).stream().limit(limit).toList();
        }
        
        if (query.length() < 2) {
            return streetRepository.findByLocalityId(localityId).stream().limit(limit).toList();
        }
        
        return streetRepository.findByLocalityIdAndNameContaining(localityId, query).stream()
            .limit(limit)
            .toList();
    }

    /**
     * Найти улицу по ID
     */
    public Street findById(Long id) {
        return streetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Улица не найдена: " + id));
    }
}
