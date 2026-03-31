package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.model.domain.Street;
import org.example.model.dto.StreetDTO;
import org.example.service.search.StreetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API для работы с улицами
 */
@RestController
@RequestMapping("/api/localities/{localityId}/streets")
public class StreetController {

    private final StreetService streetService;

    public StreetController(StreetService streetService) {
        this.streetService = streetService;
    }

    /**
     * Получить улицы по населенному пункту или поиск по названию
     * GET /api/localities/{localityId}/streets?search=Гагарина
     */
    @GetMapping
    public ApiResponse<List<StreetDTO>> getStreets(
            @PathVariable Long localityId,
            @RequestParam(required = false) String search) {
        
        List<Street> streets = streetService.searchByLocalityAndName(localityId, search, 100);
        List<StreetDTO> response = streets.stream()
            .map(s -> new StreetDTO(s.getId(), s.getName()))
            .toList();
        
        return ApiResponse.success(response);
    }

    /**
     * Получить улицу по ID
     * GET /api/localities/{localityId}/streets/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<StreetDTO> getStreet(@PathVariable Long id) {
        try {
            Street street = streetService.findById(id);
            return ApiResponse.success(new StreetDTO(street.getId(), street.getName()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("STREET_NOT_FOUND", e.getMessage());
        }
    }
}
