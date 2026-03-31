package org.example.service.search;

import org.example.model.domain.Locality;
import org.example.repository.LocalityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с населенными пунктами
 */
@Service
public class LocalityService {

    private final LocalityRepository localityRepository;

    public LocalityService(LocalityRepository localityRepository) {
        this.localityRepository = localityRepository;
    }

    /**
     * Найти населенные пункты по названию (автокомплит)
     */
    public List<Locality> searchByName(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return localityRepository.findAll().stream().limit(limit).toList();
        }
        
        if (query.length() < 2) {
            return List.of();
        }
        
        return localityRepository.findByNameContaining(query).stream()
            .limit(limit)
            .toList();
    }

    /**
     * Найти населенный пункт по ID
     */
    public Locality findById(Long id) {
        return localityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Населенный пункт не найден: " + id));
    }
}
