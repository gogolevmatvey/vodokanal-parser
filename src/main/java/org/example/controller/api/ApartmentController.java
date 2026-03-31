package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.model.domain.Apartment;
import org.example.model.dto.ApartmentDTO;
import org.example.service.search.ApartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API для работы с квартирами
 */
@RestController
@RequestMapping("/api/localities/{localityId}/streets/{streetId}/houses/{houseId}/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    /**
     * Получить квартиры по дому или поиск по номеру
     * GET /api/localities/{localityId}/streets/{streetId}/houses/{houseId}/apartments?search=45
     */
    @GetMapping
    public ApiResponse<List<ApartmentDTO>> getApartments(
            @PathVariable Long localityId,
            @PathVariable Long streetId,
            @PathVariable Long houseId,
            @RequestParam(required = false) String search) {
        
        List<Apartment> apartments = apartmentService.searchByHouseAndNumber(houseId, search, 100);
        List<ApartmentDTO> response = apartments.stream()
            .map(a -> new ApartmentDTO(a.getId(), a.getNumber()))
            .toList();
        
        return ApiResponse.success(response);
    }

    /**
     * Получить квартиру по ID
     * GET /api/localities/{localityId}/streets/{streetId}/houses/{houseId}/apartments/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ApartmentDTO> getApartment(@PathVariable Long id) {
        try {
            Apartment apartment = apartmentService.findById(id);
            return ApiResponse.success(new ApartmentDTO(apartment.getId(), apartment.getNumber()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("APARTMENT_NOT_FOUND", e.getMessage());
        }
    }
}
