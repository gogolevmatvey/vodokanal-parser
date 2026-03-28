package org.example.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.example.parser.model.dto.*;
import org.example.repository.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Сервис кэширования данных для UI
 */
public class CacheService {
    private final LoadingCache<Long, List<StreetDTO>> streetCache;
    private final LoadingCache<Long, List<HouseDTO>> houseCache;
    private final LoadingCache<Long, List<ApartmentDTO>> apartmentCache;
    
    public CacheService(StreetRepository streetRepo, HouseRepository houseRepo,
                        ApartmentRepository apartmentRepo) {
        streetCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, List<StreetDTO>>() {
                @Override
                public List<StreetDTO> load(Long localityId) {
                    return streetRepo.findByLocalityId(localityId).stream()
                        .map(s -> new StreetDTO(s.getId(), s.getName(), s.getType()))
                        .collect(Collectors.toList());
                }
            });
        
        houseCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, List<HouseDTO>>() {
                @Override
                public List<HouseDTO> load(Long streetId) {
                    return houseRepo.findByStreetId(streetId).stream()
                        .map(h -> new HouseDTO(h.getId(), h.getNumber(), h.getBuilding()))
                        .collect(Collectors.toList());
                }
            });
        
        apartmentCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, List<ApartmentDTO>>() {
                @Override
                public List<ApartmentDTO> load(Long houseId) {
                    return apartmentRepo.findByHouseId(houseId).stream()
                        .map(a -> new ApartmentDTO(a.getId(), a.getNumber()))
                        .collect(Collectors.toList());
                }
            });
    }
    
    public List<StreetDTO> getStreets(Long localityId) {
        if (localityId == null) {
            return List.of();
        }
        return streetCache.getUnchecked(localityId);
    }
    
    public List<HouseDTO> getHouses(Long streetId) {
        if (streetId == null) {
            return List.of();
        }
        return houseCache.getUnchecked(streetId);
    }
    
    public List<ApartmentDTO> getApartments(Long houseId) {
        if (houseId == null) {
            return List.of();
        }
        return apartmentCache.getUnchecked(houseId);
    }
    
    public void invalidateAll() {
        streetCache.invalidateAll();
        houseCache.invalidateAll();
        apartmentCache.invalidateAll();
    }
    
    public void invalidateStreets(Long localityId) {
        if (localityId != null) {
            streetCache.invalidate(localityId);
        }
    }
    
    public void invalidateHouses(Long streetId) {
        if (streetId != null) {
            houseCache.invalidate(streetId);
        }
    }
    
    public void invalidateApartments(Long houseId) {
        if (houseId != null) {
            apartmentCache.invalidate(houseId);
        }
    }
}
