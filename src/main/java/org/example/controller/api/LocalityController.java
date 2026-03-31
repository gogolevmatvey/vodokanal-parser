package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.model.domain.Locality;
import org.example.model.dto.LocalityDTO;
import org.example.service.search.LocalityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API для работы с населенными пунктами
 */
@RestController
@RequestMapping("/api/localities")
public class LocalityController {

    private final LocalityService localityService;

    public LocalityController(LocalityService localityService) {
        this.localityService = localityService;
    }

    /**
     * Получить все населенные пункты или поиск по названию
     * GET /api/localities?search=Владимир
     */
    @GetMapping
    public ApiResponse<List<LocalityDTO>> getLocalities(
            @RequestParam(required = false) String search) {
        
        List<Locality> localities = localityService.searchByName(search, 100);
        List<LocalityDTO> response = localities.stream()
            .map(l -> new LocalityDTO(l.getId(), l.getName()))
            .toList();
        
        return ApiResponse.success(response);
    }

    /**
     * Получить населенный пункт по ID
     * GET /api/localities/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<LocalityDTO> getLocality(@PathVariable Long id) {
        try {
            Locality locality = localityService.findById(id);
            return ApiResponse.success(new LocalityDTO(locality.getId(), locality.getName()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("LOCALITY_NOT_FOUND", e.getMessage());
        }
    }
}
