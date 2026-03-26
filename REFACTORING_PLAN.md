# План рефакторинга проекта Vodokanal Parser

## 📋 Оценка текущей архитектуры

### ✅ Сильные стороны

| Аспект | Оценка | Комментарий |
|--------|--------|-------------|
| **Разделение ответственности** | ⭐⭐⭐⭐ | Есть репозитории (Address, Account, Billing) |
| **Работа с БД** | ⭐⭐⭐⭐ | HikariCP, PreparedStatement |
| **Тестирование** | ⭐⭐⭐⭐⭐ | 57 тестов, хорошее покрытие |
| **Обработка ошибок** | ⭐⭐⭐⭐ | Детальная статистика, логирование |

### ❌ Проблемные места

| Проблема | Критичность | Влияние на десктоп |
|----------|-------------|---------------------|
| **Вся логика в Main.java** | 🔴 Высокая | Невозможно переиспользовать |
| **Нет слоя сервисов** | 🔴 Высокая | Бизнес-логика перемешана с UI |
| **Репозитории без интерфейсов** | 🟡 Средняя | Сложно тестировать и заменять |
| **Нет DTO для записей** | 🟡 Средняя | RecordData используется только внутри |
| **Прямая зависимость от PostgreSQL** | 🟡 Средняя | Сложно изменить БД |
| **Нет кэширования справочников** | 🟡 Средняя | Для dropdowns нужно быстрое получение |
| **Статические методы** | 🟡 Средняя | Сложно внедрять зависимости |

---

## 📐 Целевая архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                    Desktop UI (JavaFX/Swing)                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │Locality  │ →│ Street   │ →│ House    │ →│Apartment │ → ... │
│  │Dropdown  │ │Dropdown  │ │Dropdown  │ │Dropdown  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                      Service Layer                               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │SearchService   │  │ParsingService  │  │ImportService   │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                     Repository Layer                             │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │LocalityRepo│ │StreetRepo  │ │HouseRepo   │ │AccountRepo │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │ApartmentRep│ │BillingRepo │ │MeterRepo   │ │...         │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                    Database (PostgreSQL)                         │
│  localities | streets | houses | apartments | accounts | ...   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 План рефакторинга

### Этап 1: Подготовка инфраструктуры (2-3 дня)

#### 1.1 Создать структуру пакетов

```
src/main/java/org/example/
├── Main.java                          # Точка входа
├── parser/                            # Парсинг данных
│   ├── RecordParser.java              # Парсинг записей
│   ├── RecordCorrector.java           # Исправление ошибок
│   ├── RecordValidator.java           # Валидация
│   └── model/                         # DTO
│       ├── RawRecord.java             # Сырая запись
│       ├── ParsedRecord.java          # Распаршенная запись
│       └── ParseResult.java           # Результат парсинга
├── repository/                        # Доступ к данным
│   ├── BaseRepository.java            # Базовый интерфейс
│   ├── LocalityRepository.java        # Населенные пункты
│   ├── StreetRepository.java          # Улицы
│   ├── HouseRepository.java           # Дома
│   ├── ApartmentRepository.java       # Квартиры
│   ├── AccountRepository.java         # Лицевые счета
│   ├── BillingPeriodRepository.java   # Периоды
│   └── MeterChargeRepository.java     # Приборы
├── service/                           # Бизнес-логика
│   ├── ParsingService.java            # Импорт файлов
│   ├── SearchService.java             # Поиск для UI
│   └── StatisticsService.java         # Статистика
├── database/                          # Работа с БД
│   ├── DatabaseManager.java           # Пул соединений
│   └── DatabaseInitializer.java       # Инициализация схем
└── exception/                         # Исключения
    ├── RecordParseException.java
    ├── DatabaseException.java
    └── NotFoundException.java
```

#### 1.2 Добавить зависимости (без Lombok)

```xml
<!-- Jackson для JSON (если понадобится для UI) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- Guava для кэширования -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.2.1-jre</version>
</dependency>
```

---

### Этап 2: Создание DTO моделей (2 дня)

#### 2.1 Базовые модели

```java
// parser/model/Locality.java
package org.example.parser.model;

import java.util.Objects;

/**
 * Населенный пункт (город, село, поселок, деревня)
 */
public class Locality {
    private Long id;
    private String name;
    private String type; // "г", "с", "п", "дер"
    
    public Locality() {}
    
    public Locality(Long id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locality locality = (Locality) o;
        return Objects.equals(id, locality.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    // toString
    @Override
    public String toString() {
        return "Locality{id=" + id + ", name='" + name + "', type='" + type + "'}";
    }
}
```

```java
// parser/model/Street.java
package org.example.parser.model;

import java.util.Objects;

/**
 * Улица
 */
public class Street {
    private Long id;
    private Long localityId;
    private String name;
    private String type; // "ул", "пер", "пр-кт"
    
    public Street() {}
    
    public Street(Long id, Long localityId, String name, String type) {
        this.id = id;
        this.localityId = localityId;
        this.name = name;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLocalityId() { return localityId; }
    public void setLocalityId(Long localityId) { this.localityId = localityId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Street street = (Street) o;
        return Objects.equals(id, street.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Street{id=" + id + ", name='" + name + "', type='" + type + "'}";
    }
}
```

```java
// parser/model/House.java
package org.example.parser.model;

import java.util.Objects;

/**
 * Дом
 */
public class House {
    private Long id;
    private Long streetId;
    private String number;
    private String building; // "корп.", "стр."
    
    public House() {}
    
    public House(Long id, Long streetId, String number, String building) {
        this.id = id;
        this.streetId = streetId;
        this.number = number;
        this.building = building;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStreetId() { return streetId; }
    public void setStreetId(Long streetId) { this.streetId = streetId; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        House house = (House) o;
        return Objects.equals(id, house.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "House{id=" + id + ", number='" + number + "', building='" + building + "'}";
    }
}
```

```java
// parser/model/Apartment.java
package org.example.parser.model;

import java.util.Objects;

/**
 * Квартира/помещение
 */
public class Apartment {
    private Long id;
    private Long houseId;
    private String number;
    
    public Apartment() {}
    
    public Apartment(Long id, Long houseId, String number) {
        this.id = id;
        this.houseId = houseId;
        this.number = number;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getHouseId() { return houseId; }
    public void setHouseId(Long houseId) { this.houseId = houseId; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Apartment apartment = (Apartment) o;
        return Objects.equals(id, apartment.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Apartment{id=" + id + ", number='" + number + "'}";
    }
}
```

```java
// parser/model/Account.java
package org.example.parser.model;

import java.util.Objects;

/**
 * Лицевой счет
 */
public class Account {
    private Long id;
    private Long apartmentId;
    private String accountNumber;
    private String payerName;
    
    public Account() {}
    
    public Account(Long id, Long apartmentId, String accountNumber, String payerName) {
        this.id = id;
        this.apartmentId = apartmentId;
        this.accountNumber = accountNumber;
        this.payerName = payerName;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getApartmentId() { return apartmentId; }
    public void setApartmentId(Long apartmentId) { this.apartmentId = apartmentId; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    
    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Account{id=" + id + ", accountNumber='" + accountNumber + "', payerName='" + payerName + "'}";
    }
}
```

#### 2.2 DTO для UI

```java
// parser/model/dto/LocalityDTO.java
package org.example.parser.model.dto;

/**
 * DTO для отображения населенного пункта в UI
 */
public class LocalityDTO {
    private final Long id;
    private final String displayName;
    
    public LocalityDTO(Long id, String name, String type) {
        this.id = id;
        this.displayName = formatDisplayName(name, type);
    }
    
    private String formatDisplayName(String name, String type) {
        if (type == null || type.isEmpty()) {
            return name;
        }
        return type + ". " + name;
    }
    
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    
    @Override
    public String toString() {
        return displayName;
    }
}
```

```java
// parser/model/dto/StreetDTO.java
package org.example.parser.model.dto;

/**
 * DTO для отображения улицы в UI
 */
public class StreetDTO {
    private final Long id;
    private final String displayName;
    
    public StreetDTO(Long id, String name, String type) {
        this.id = id;
        this.displayName = formatDisplayName(name, type);
    }
    
    private String formatDisplayName(String name, String type) {
        if (type == null || type.isEmpty()) {
            return name;
        }
        return type + ". " + name;
    }
    
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    
    @Override
    public String toString() {
        return displayName;
    }
}
```

```java
// parser/model/dto/FullAccountDTO.java
package org.example.parser.model.dto;

import java.util.List;

/**
 * Полная информация о лицевом счете для отображения в UI
 */
public class FullAccountDTO {
    private final Long accountId;
    private final String accountNumber;
    private final String payerName;
    private final String fullAddress;
    private final List<BillingPeriodDTO> periods;
    
    public FullAccountDTO(Long accountId, String accountNumber, String payerName, 
                          String fullAddress, List<BillingPeriodDTO> periods) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.payerName = payerName;
        this.fullAddress = fullAddress;
        this.periods = periods;
    }
    
    public Long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getPayerName() { return payerName; }
    public String getFullAddress() { return fullAddress; }
    public List<BillingPeriodDTO> getPeriods() { return periods; }
}
```

---

### Этап 3: Выделение слоя репозиториев (3-4 дня)

#### 3.1 Базовый интерфейс

```java
// repository/BaseRepository.java
package org.example.repository;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс репозитория
 * @param <T> Тип сущности
 * @param <ID> Тип идентификатора
 */
public interface BaseRepository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
}
```

#### 3.2 Интерфейсы репозиториев

```java
// repository/LocalityRepository.java
package org.example.repository;

import org.example.parser.model.Locality;

import java.util.List;
import java.util.Optional;

public interface LocalityRepository extends BaseRepository<Locality, Long> {
    List<Locality> findByNameContaining(String name);
    Optional<Locality> findByName(String name);
}
```

```java
// repository/StreetRepository.java
package org.example.repository;

import org.example.parser.model.Street;

import java.util.List;

public interface StreetRepository extends BaseRepository<Street, Long> {
    List<Street> findByLocalityId(Long localityId);
    List<Street> findByLocalityIdAndNameContaining(Long localityId, String name);
}
```

```java
// repository/HouseRepository.java
package org.example.repository;

import org.example.parser.model.House;

import java.util.List;

public interface HouseRepository extends BaseRepository<House, Long> {
    List<House> findByStreetId(Long streetId);
    List<House> findByStreetIdAndNumberContaining(Long streetId, String number);
}
```

```java
// repository/ApartmentRepository.java
package org.example.repository;

import org.example.parser.model.Apartment;

import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends BaseRepository<Apartment, Long> {
    List<Apartment> findByHouseId(Long houseId);
    List<Apartment> findByHouseIdAndNumberContaining(Long houseId, String number);
    Optional<Apartment> findByHouseIdAndNumber(Long houseId, String number);
}
```

```java
// repository/AccountRepository.java
package org.example.repository;

import org.example.parser.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByApartmentId(Long apartmentId);
    Optional<Account> findByApartmentIdAndAccountNumber(Long apartmentId, String accountNumber);
}
```

#### 3.3 Реализации репозиториев

```java
// repository/impl/JdbcLocalityRepository.java
package org.example.repository.impl;

import org.example.parser.model.Locality;
import org.example.repository.LocalityRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLocalityRepository implements LocalityRepository {
    private final DataSource dataSource;
    
    public JdbcLocalityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<Locality> findById(Long id) {
        String sql = "SELECT id, name, type FROM localities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding locality by id", e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Locality> findAll() {
        String sql = "SELECT id, name, type FROM localities ORDER BY name";
        List<Locality> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all localities", e);
        }
        return result;
    }
    
    @Override
    public List<Locality> findByNameContaining(String name) {
        String sql = "SELECT id, name, type FROM localities WHERE name ILIKE ? ORDER BY name LIMIT 100";
        List<Locality> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding localities by name", e);
        }
        return result;
    }
    
    @Override
    public Optional<Locality> findByName(String name) {
        String sql = "SELECT id, name, type FROM localities WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding locality by name", e);
        }
        return Optional.empty();
    }
    
    @Override
    public Locality save(Locality entity) {
        String sql = "INSERT INTO localities (name, type) VALUES (?, ?) " +
                     "ON CONFLICT (name) DO UPDATE SET type = EXCLUDED.type " +
                     "RETURNING id, name, type";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getName());
            stmt.setString(2, entity.getType());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving locality", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM localities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting locality", e);
        }
    }
    
    private Locality mapRow(ResultSet rs) throws SQLException {
        Locality locality = new Locality();
        locality.setId(rs.getLong("id"));
        locality.setName(rs.getString("name"));
        locality.setType(rs.getString("type"));
        return locality;
    }
}
```

---

### Этап 4: Создание слоя сервисов (2-3 дня)

#### 4.1 SearchService для UI

```java
// service/SearchService.java
package org.example.service;

import org.example.parser.model.*;
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
        List<Locality> localities;
        if (query == null || query.trim().isEmpty()) {
            localities = localityRepo.findAll();
        } else {
            localities = localityRepo.findByNameContaining(query.trim());
        }
        return localities.stream()
            .map(l -> new LocalityDTO(l.getId(), l.getName(), l.getType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Поиск улиц для dropdown по выбранному населенному пункту
     */
    public List<StreetDTO> searchStreets(Long localityId, String query) {
        List<Street> streets;
        if (query == null || query.trim().isEmpty()) {
            streets = streetRepo.findByLocalityId(localityId);
        } else {
            streets = streetRepo.findByLocalityIdAndNameContaining(localityId, query.trim());
        }
        return streets.stream()
            .map(s -> new StreetDTO(s.getId(), s.getName(), s.getType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Поиск домов для dropdown по выбранной улице
     */
    public List<HouseDTO> searchHouses(Long streetId, String query) {
        List<House> houses;
        if (query == null || query.trim().isEmpty()) {
            houses = houseRepo.findByStreetId(streetId);
        } else {
            houses = houseRepo.findByStreetIdAndNumberContaining(streetId, query.trim());
        }
        return houses.stream()
            .map(h -> new HouseDTO(h.getId(), h.getNumber(), h.getBuilding()))
            .collect(Collectors.toList());
    }
    
    /**
     * Поиск квартир для dropdown по выбранному дому
     */
    public List<ApartmentDTO> searchApartments(Long houseId, String query) {
        List<Apartment> apartments;
        if (query == null || query.trim().isEmpty()) {
            apartments = apartmentRepo.findByHouseId(houseId);
        } else {
            apartments = apartmentRepo.findByHouseIdAndNumberContaining(houseId, query.trim());
        }
        return apartments.stream()
            .map(a -> new ApartmentDTO(a.getId(), a.getNumber()))
            .collect(Collectors.toList());
    }
    
    /**
     * Поиск лицевых счетов по выбранной квартире
     */
    public List<AccountDTO> searchAccounts(Long apartmentId, String query) {
        List<Account> accounts;
        if (query == null || query.trim().isEmpty()) {
            accounts = accountRepo.findByApartmentId(apartmentId);
        } else {
            // Нужно добавить метод в репозиторий
            accounts = accountRepo.findByApartmentId(apartmentId);
        }
        return accounts.stream()
            .map(a -> new AccountDTO(a.getId(), a.getAccountNumber(), a.getPayerName()))
            .collect(Collectors.toList());
    }
    
    /**
     * Получение полной информации о лицевом счете
     */
    public FullAccountDTO getFullAccountInfo(Long accountId) {
        // Реализация получения полной информации
        return null;
    }
}
```

#### 4.2 ParsingService для импорта

```java
// service/ParsingService.java
package org.example.service;

import org.example.parser.RecordParser;
import org.example.parser.RecordCorrector;
import org.example.parser.model.ParsedRecord;
import org.example.repository.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для парсинга и импорта файлов
 */
public class ParsingService {
    private final RecordParser parser;
    private final RecordCorrector corrector;
    private final AccountRepository accountRepo;
    private final BillingPeriodRepository billingRepo;
    private final MeterChargeRepository meterRepo;
    
    public ParsingService(RecordParser parser, RecordCorrector corrector,
                          AccountRepository accountRepo, BillingPeriodRepository billingRepo,
                          MeterChargeRepository meterRepo) {
        this.parser = parser;
        this.corrector = corrector;
        this.accountRepo = accountRepo;
        this.billingRepo = billingRepo;
        this.meterRepo = meterRepo;
    }
    
    /**
     * Парсинг и импорт файла
     */
    public ParsingResult parseAndImport(File file) throws IOException {
        AtomicInteger validCount = new AtomicInteger();
        AtomicInteger invalidCount = new AtomicInteger();
        AtomicInteger correctedCount = new AtomicInteger();
        AtomicInteger dbErrorCount = new AtomicInteger();
        
        Files.lines(file.toPath())
            .forEach(line -> {
                if (line.trim().isEmpty()) {
                    return;
                }
                
                ParsedRecord record = parser.parse(line);
                if (record.isValid()) {
                    validCount.incrementAndGet();
                    // Сохранение в БД
                } else {
                    invalidCount.incrementAndGet();
                    String corrected = corrector.correct(line, record.getErrorMessage());
                    if (corrected != null) {
                        correctedCount.incrementAndGet();
                    }
                }
            });
        
        return new ParsingResult(
            validCount.get(),
            invalidCount.get(),
            correctedCount.get(),
            dbErrorCount.get()
        );
    }
}
```

---

### Этап 5: Рефакторинг Main.java (1-2 дня)

#### 5.1 Оставить Main.java как точку входа

```java
// Main.java
package org.example;

import org.example.database.DatabaseManager;
import org.example.service.ParsingService;
import org.example.service.SearchService;

import java.io.File;

/**
 * Точка входа в приложение
 */
public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        
        // Инициализация репозиториев
        LocalityRepository localityRepo = new JdbcLocalityRepository(dbManager.getDataSource());
        // ... другие репозитории
        
        // Инициализация сервисов
        ParsingService parsingService = new ParsingService(/* ... */);
        SearchService searchService = new SearchService(/* ... */);
        
        // Импорт файлов (старый функционал)
        if (args.length > 0) {
            parsingService.parseAndImport(new File(args[0]));
        }
        
        // Запуск UI (новый функционал)
        // DesktopUI.launch(searchService);
    }
}
```

---

### Этап 6: Подготовка к UI (2-3 дня)

#### 6.1 Кэширование для dropdowns

```java
// service/CacheService.java
package org.example.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.example.parser.model.dto.*;
import org.example.repository.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        return streetCache.getUnchecked(localityId);
    }
    
    public List<HouseDTO> getHouses(Long streetId) {
        return houseCache.getUnchecked(streetId);
    }
    
    public List<ApartmentDTO> getApartments(Long houseId) {
        return apartmentCache.getUnchecked(houseId);
    }
    
    public void invalidateAll() {
        streetCache.invalidateAll();
        houseCache.invalidateAll();
        apartmentCache.invalidateAll();
    }
}
```

---

## 📅 Итоговый план

| Этап | Задачи | Время | Приоритет |
|------|--------|-------|-----------|
| **1. Инфраструктура** | Пакеты, зависимости | 2-3 дня | 🔴 Высокий |
| **2. DTO модели** | Создание всех DTO | 2 дня | 🔴 Высокий |
| **3. Репозитории** | Интерфейсы, реализации | 3-4 дня | 🔴 Высокий |
| **4. Сервисы** | SearchService, ParsingService | 2-3 дня | 🔴 Высокий |
| **5. Рефакторинг Main** | Выделение логики | 1-2 дня | 🟡 Средний |
| **6. Подготовка к UI** | DTO, кэширование | 2-3 дня | 🟡 Средний |
| **7. Тестирование** | Обновление тестов | 2-3 дня | 🔴 Высокий |

**Итого:** 14-20 дней

---

## ✅ Критерии успеха

После рефакторинга:

1. ✅ **Весь старый функционал работает** (импорт файлов, статистика)
2. ✅ **Сервисы готовы для UI** (SearchService с методами для dropdowns)
3. ✅ **Покрытие тестов ≥ 80%** (сейчас ~70%)
4. ✅ **Время отклика dropdowns < 100ms** (кэширование)
5. ✅ **Возможность легкой замены БД** (интерфейсы репозиториев)
6. ✅ **Чистая архитектура** (разделение слоёв)
7. ✅ **Нет Lombok** (все getter/setter написаны вручную)

---

## 🚀 Рекомендации для UI

Для десктопного интерфейса рекомендую:

| Технология | Плюсы | Минусы |
|------------|-------|--------|
| **JavaFX** | Современный, богатый UI | Требует отдельной установки |
| **Swing + FlatLaf** | Встроен в JDK, простой | Устаревший вид |
| **Compose Multiplatform** | Кроссплатформенный | Новый, мало примеров |

**Рекомендация:** JavaFX + JFoenix (Material Design) для современного вида.

---

## 📝 Примечания

1. **Lombok не используется** - все getter/setter/equals/hashCode написаны вручную
2. **Сохранена обратная совместимость** - старый функционал импорта работает
3. **Готовность к UI** - SearchService предоставляет данные для cascading dropdowns
4. **Кэширование** - Guava Cache для быстрого получения справочников
5. **Тестируемость** - интерфейсы репозиториев позволяют мокать зависимости
