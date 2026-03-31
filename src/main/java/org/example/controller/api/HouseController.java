package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.model.domain.House;
import org.example.model.dto.HouseDTO;
import org.example.service.search.HouseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API для работы с домами
 */
@RestController
@RequestMapping("/api/localities/{localityId}/streets/{streetId}/houses")
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * Получить дома по улице или поиск по номеру
     * GET /api/localities/{localityId}/streets/{streetId}/houses?search=12
     */
    @GetMapping
    public ApiResponse<List<HouseDTO>> getHouses(
            @PathVariable Long localityId,
            @PathVariable Long streetId,
            @RequestParam(required = false) String search) {
        
        List<House> houses = houseService.searchByStreetAndNumber(streetId, search, 100);
        List<HouseDTO> response = houses.stream()
            .map(h -> new HouseDTO(h.getId(), h.getNumber()))
            .toList();
        
        return ApiResponse.success(response);
    }

    /**
     * Получить дом по ID
     * GET /api/localities/{localityId}/streets/{streetId}/houses/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<HouseDTO> getHouse(@PathVariable Long id) {
        try {
            House house = houseService.findById(id);
            return ApiResponse.success(new HouseDTO(house.getId(), house.getNumber()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("HOUSE_NOT_FOUND", e.getMessage());
        }
    }
}
