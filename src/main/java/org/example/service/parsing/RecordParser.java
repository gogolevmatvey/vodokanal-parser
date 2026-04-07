package org.example.service.parsing;

import org.example.model.common.ParseResult;
import org.example.model.common.ParsedRecord;
import org.example.model.common.Address;
import org.example.model.domain.MeterCharge;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Парсер записей
 */
@Service
public class RecordParser {
    
    /**
     * Распарсить строку
     */
    public ParseResult parse(String line) {
        try {
            String[] parts = line.split(";");

            // Проверяем минимальное количество частей
            if (parts.length < 5) {
                return new ParseResult(false, "Недостаточно полей в строке", null);
            }

            // 1. Номер лицевого счета
            String accountNumber = parts[0].trim();
            if (accountNumber.isEmpty()) {
                return new ParseResult(false, "Пустой номер лицевого счета", null);
            }
            if (!accountNumber.matches("^\\d+$")) {
                return new ParseResult(false, "Неверный формат номера лицевого счета", null);
            }

            // 2. ФИО плательщика
            String payerName = parts[1].trim();
            if (payerName.isEmpty() || payerName.matches("\\s*") || payerName.equals(".*") || payerName.matches("^[.\\*\\s\\d]+$")) {
                return new ParseResult(false, "Пустое ФИО плательщика", null);
            }

            // 3. Адрес
            String addressStr = parts[2].trim();
            if (addressStr.isEmpty()) {
                return new ParseResult(false, "Пустой адрес", null);
            }

            int commaCount = countChar(addressStr, ',');
            if (commaCount < 3) {
                return new ParseResult(false, "Неполный адрес (меньше 3 запятых)", null);
            }

            if (commaCount > 5) {
                return new ParseResult(false, "Подозрительный адрес (больше 5 запятых)", null);
            }

            String[] addressParts = addressStr.split(",");

            for (int i = 0; i < Math.min(3, addressParts.length); i++) {
                if (addressParts[i].trim().isEmpty()) {
                    return new ParseResult(false, "Пустая часть адреса", null);
                }
            }

            String locality = addressParts[0].trim();
            String street = addressParts[1].trim();
            String house = addressParts[2].trim();

            List<String> apartments = new ArrayList<>();
            for (int i = 3; i < addressParts.length; i++) {
                String apt = addressParts[i].trim();
                if (!apt.isEmpty()) {
                    apartments.add(apt);
                }
            }

            // 4. Период начисления
            String billingPeriod = parts[3].trim();
            if (billingPeriod.isEmpty()) {
                return new ParseResult(false, "Пустой период начисления", null);
            }
            if (!billingPeriod.matches("^\\d{2,4}$") && isTextualMonth(billingPeriod)) {
                return new ParseResult(false, "Период в текстовом формате", null);
            }

            // 5. Суммы начисления, приборы учета и показания
            List<MeterCharge> meterCharges = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            if (parts.length == 5) {
                try {
                    double amount = Double.parseDouble(parts[4].trim());
                    totalAmount = BigDecimal.valueOf(amount);
                    MeterCharge charge = new MeterCharge(null, null, BigDecimal.valueOf(amount));
                    meterCharges.add(charge);
                } catch (NumberFormatException e) {
                    return new ParseResult(false, "Неверный формат суммы начисления: " + parts[4], null);
                }
            } else {
                try {
                    double amount = Double.parseDouble(parts[4].trim());
                    totalAmount = BigDecimal.valueOf(amount);

                    List<String> meters = new ArrayList<>();
                    List<Double> readings = new ArrayList<>();

                    for (int i = 5; i < parts.length; i += 2) {
                        String meter = parts[i].trim();
                        if (meter.isEmpty()) {
                            meter = null;
                        } else if (!isValidMeterOrReading(meter)) {
                            if (isTextualMonth(meter)) {
                                return new ParseResult(false, "Текстовый месяц в поле данных: " + meter, null);
                            }
                            return new ParseResult(false, "Подозрительное значение прибора учета: " + meter, null);
                        }

                        Double reading = null;
                        if (i + 1 < parts.length) {
                            String readingStr = parts[i + 1].trim();
                            if (!readingStr.isEmpty()) {
                                if (isTextualMonth(readingStr)) {
                                    return new ParseResult(false, "Текстовый месяц в поле показаний: " + readingStr, null);
                                }
                                if (!isValidMeterOrReading(readingStr)) {
                                    return new ParseResult(false, "Неверный формат показаний: " + readingStr, null);
                                }
                                try {
                                    reading = Double.parseDouble(readingStr);
                                } catch (NumberFormatException e) {
                                    return new ParseResult(false, "Неверный формат показаний: " + readingStr, null);
                                }
                            }
                        }

                        meters.add(meter);
                        readings.add(reading);
                    }

                    if (!meters.isEmpty()) {
                        for (int i = 0; i < meters.size(); i++) {
                            MeterCharge charge = new MeterCharge(
                                meters.get(i),
                                readings.get(i) != null ? BigDecimal.valueOf(readings.get(i)) : null,
                                BigDecimal.valueOf(amount)
                            );
                            meterCharges.add(charge);
                        }
                    } else {
                        MeterCharge charge = new MeterCharge(null, null, BigDecimal.valueOf(amount));
                        meterCharges.add(charge);
                    }
                } catch (NumberFormatException e) {
                    return new ParseResult(false, "Неверный формат суммы начисления: " + parts[4], null);
                }
            }

            Address address = new Address(null, null, null, null, 
                locality + ", " + street + ", " + house + 
                (apartments.isEmpty() ? "" : ", " + String.join(", ", apartments)));

            ParsedRecord recordData = new ParsedRecord(
                    accountNumber,
                    payerName,
                    address,
                    billingPeriod,
                    totalAmount,
                    meterCharges
            );

            return new ParseResult(true, null, recordData);

        } catch (Exception e) {
            return new ParseResult(false, "Ошибка парсинга строки: " + e.getMessage(), null);
        }
    }
    
    /**
     * Подсчитывает количество вхождений символа в строку
     */
    private int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Проверяет, является ли значение допустимым для прибора учета или показаний
     */
    private boolean isValidMeterOrReading(String value) {
        if (value.matches("^\\d+\\.?\\d*$") || value.matches("^\\d*\\.\\d+$")) {
            return true;
        }

        if (value.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+().]+.$") ||
            value.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+().]+$")) {
            return true;
        }

        return value.equals(".*");
    }
    
    /**
     * Проверяет, является ли строка текстовым месяцем
     */
    private boolean isTextualMonth(String s) {
        return s.matches("^(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)\\.?\\d{2}$") ||
               s.matches("^\\d{2}\\.(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)$");
    }
}
