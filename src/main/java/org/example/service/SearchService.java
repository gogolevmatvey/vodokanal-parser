package org.example.service;

import org.example.parser.model.dto.*;
import org.example.repository.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для поиска данных для UI
 */
public class SearchService {
    private final LocalityRepository localityRepo;
    private final StreetRepository streetRepo;
    private final HouseRepository houseRepo;
    private final ApartmentRepository apartmentRepo;
    private final AccountRepository accountRepo;
    
    public SearchService(LocalityRepository localityRepo, StreetRepository streetRepo,
                         HouseRepository houseRepo, ApartmentRepository apartmentRepo,
                         AccountRepository accountRepo) {
        this.localityRepo = localityRepo;
        this.streetRepo = streetRepo;
        this.houseRepo = houseRepo;
        this.apartmentRepo = apartmentRepo;
        this.accountRepo = accountRepo;
    }
    
    /**
     * Поиск населенных пунктов для dropdown
     */
    public List<LocalityDTO> searchLocalities(String query) {
        List<LocalityDTO> result = localityRepo.findAll().stream()
            .map(l -> new LocalityDTO(l.getId(), l.getName(), l.getType()))
            .collect(Collectors.toList());
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            return result.stream()
                .filter(l -> l.getDisplayName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
        
        return result;
    }
    
    /**
     * Поиск улиц для dropdown по выбранному населенному пункту
     */
    public List<StreetDTO> searchStreets(Long localityId, String query) {
        if (localityId == null) {
            return List.of();
        }
        
        List<StreetDTO> result = streetRepo.findByLocalityId(localityId).stream()
            .map(s -> new StreetDTO(s.getId(), s.getName(), s.getType()))
            .collect(Collectors.toList());
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            return result.stream()
                .filter(s -> s.getDisplayName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
        
        return result;
    }
    
    /**
     * Поиск домов для dropdown по выбранной улице
     */
    public List<HouseDTO> searchHouses(Long streetId, String query) {
        if (streetId == null) {
            return List.of();
        }
        
        List<HouseDTO> result = houseRepo.findByStreetId(streetId).stream()
            .map(h -> new HouseDTO(h.getId(), h.getNumber(), h.getBuilding()))
            .collect(Collectors.toList());
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            return result.stream()
                .filter(h -> h.getDisplayNumber().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
        
        return result;
    }
    
    /**
     * Поиск квартир для dropdown по выбранному дому
     */
    public List<ApartmentDTO> searchApartments(Long houseId, String query) {
        if (houseId == null) {
            return List.of();
        }
        
        List<ApartmentDTO> result = apartmentRepo.findByHouseId(houseId).stream()
            .map(a -> new ApartmentDTO(a.getId(), a.getNumber()))
            .collect(Collectors.toList());
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            return result.stream()
                .filter(a -> a.getNumber().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
        
        return result;
    }
    
    /**
     * Поиск лицевых счетов по выбранной квартире
     */
    public List<AccountDTO> searchAccounts(Long apartmentId, String query) {
        if (apartmentId == null) {
            return List.of();
        }
        
        List<AccountDTO> result = accountRepo.findByApartmentId(apartmentId).stream()
            .map(a -> new AccountDTO(a.getId(), a.getAccountNumber(), a.getPayerName()))
            .collect(Collectors.toList());
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.trim().toLowerCase();
            return result.stream()
                .filter(a -> a.getAccountNumber().toLowerCase().contains(lowerQuery) ||
                            a.getPayerName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
        
        return result;
    }
}
