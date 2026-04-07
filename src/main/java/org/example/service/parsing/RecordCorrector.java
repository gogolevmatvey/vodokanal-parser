package org.example.service.parsing;

import org.springframework.stereotype.Service;

/**
 * Корректор записей
 */
@Service
public class RecordCorrector {
    
    /**
     * Исправить запись
     */
    public String correct(String line, String errorMessage) {
        String[] parts = line.split(";");

        // Если недостаточно полей - пытаемся восстановить
        if (parts.length < 5) {
            boolean isAddressInFioField = false;
            if (parts.length >= 2) {
                String field2 = parts[1].trim();
                if (field2.contains(",")) {
                    isAddressInFioField = true;
                } else if (field2.matches(".*\\s[сспгдкм]\\.?$") ||
                         field2.contains(" с ") || field2.contains(" п ") ||
                         field2.contains(" г ") || field2.contains(" дер") ||
                         field2.contains("мкр") || field2.contains("р-н")) {
                    if (parts.length >= 3) {
                        String field3 = parts[2].trim();
                        if (field3.contains("ул") || field3.matches(".*\\d+.*")) {
                            isAddressInFioField = true;
                        }
                    }
                }
            }

            if (isAddressInFioField) {
                String[] newParts = new String[parts.length + 1];
                newParts[0] = parts[0];
                newParts[1] = "данные отсутствуют";
                System.arraycopy(parts, 1, newParts, 2, parts.length - 1);
                parts = newParts;
            } else {
                while (parts.length < 5) {
                    String[] newParts = new String[parts.length + 1];
                    System.arraycopy(parts, 0, newParts, 0, parts.length);
                    newParts[parts.length] = "";
                    parts = newParts;
                }
            }
        }
        // Если 5+ полей, но ошибка "Неполный адрес"
        else if (parts.length >= 5 && errorMessage.contains("Неполный адрес")) {
            String field2 = parts[1].trim();
            int commaCount = countChar(field2, ',');
            if (field2.contains(" с ") || field2.contains(" п ") || field2.contains(" г ") ||
                field2.contains(" дер") || field2.contains("мкр") || field2.contains("р-н") ||
                field2.matches(".*\\s[сспгдкм]\\.?$") || commaCount >= 3) {
                String[] newParts = new String[parts.length + 1];
                newParts[0] = parts[0];
                newParts[1] = "данные отсутствуют";
                System.arraycopy(parts, 1, newParts, 2, parts.length - 1);
                parts = newParts;
            }
        }

        // Исправление ФИО
        String payerName = parts[1].trim();
        if (payerName.isEmpty() ||
            payerName.equals(".*") ||
            payerName.equals(".") ||
            payerName.matches("^[\\s*]+$") ||
            payerName.matches("^\\d\\s+\\*\\s+\\*$") ||
            payerName.matches("^\\d\\s+\\*+\\s+\\*\\s+\\*$") ||
            payerName.matches("^[.\\s]+\\*\\s+\\*$") ||
            payerName.matches("^[.\\s]+\\*$") ||
            payerName.matches("^\\d+\\*+\\s+\\*+\\s+\\*+$") ||
            payerName.matches("^\\d+\\s+\\*+\\s+\\*+$") ||
            payerName.matches("^[.]+\\*+([\\s]+\\*+)+$") ||
            payerName.matches("^\\d+\\s+\\*+$") ||
            payerName.matches("^\\d+\\*+([\\s]+\\*+)+$") ||
            payerName.matches("^[.\\s*]+$") ||
            payerName.matches("^\\d+\\*+$")) {
            parts[1] = "данные отсутствуют";
        }

        // Исправление адреса
        if (parts.length > 2) {
            String addressStr = parts[2].trim();
            String[] addressParts = addressStr.split(",");
            int commaCount = countChar(addressStr, ',');

            if (commaCount < 3) {
                String locality = addressParts.length > 0 ? addressParts[0].trim() : "Населенный пункт не указан";
                String street = addressParts.length > 1 ? addressParts[1].trim() : "Улица не указана";

                if (commaCount == 0) {
                    parts[2] = locality + ", Улица не указана, Дом не указан, Квартира не указана";
                } else if (commaCount == 1) {
                    parts[2] = locality + ", " + street + ", Дом не указан, Квартира не указана";
                } else if (commaCount == 2) {
                    String part1 = addressParts[0].trim();
                    String part2 = addressParts[1].trim();
                    String part3 = addressParts[2].trim();

                    boolean isFirstPartLocality = part1.matches(".*\\s[сспгдкм]\\.?$") ||
                                                  part1.contains(" с ") || part1.contains(" п ") ||
                                                  part1.contains(" г ") || part1.contains(" дер") ||
                                                  part1.contains("мкр") || part1.contains("р-н");

                    if (isFirstPartLocality) {
                        if (part3.matches("^\\d+$")) {
                            parts[2] = part1 + ", Улица не указана, " + part2 + ", " + part3;
                        } else {
                            parts[2] = part1 + ", " + part2 + ", " + part3 + ", Квартира не указана";
                        }
                    } else {
                        parts[2] = "Населенный пункт не указан, " + part1 + ", " + part2 + ", " + part3;
                    }
                }
            } else if (commaCount > 3) {
                String locality = addressParts[0].trim();
                String street = addressParts[1].trim();
                String house = addressParts[2].trim();

                StringBuilder apartmentBuilder = new StringBuilder();
                for (int i = 3; i < addressParts.length; i++) {
                    if (apartmentBuilder.length() > 0) {
                        apartmentBuilder.append(" ");
                    }
                    apartmentBuilder.append(addressParts[i].trim());
                }
                String apartment = apartmentBuilder.toString();
                parts[2] = locality + ", " + street + ", " + house + ", " + apartment;
            }
        }

        // Исправление периода
        if (parts.length > 3) {
            String billingPeriod = parts[3].trim();
            if (billingPeriod.isEmpty() || isTextualMonth(billingPeriod) || !billingPeriod.matches("^\\d+$")) {
                parts[3] = "0";
            }
        }

        // Исправление суммы
        if (parts.length > 4) {
            String amountStr = parts[4].trim();
            if (amountStr.isEmpty() || isTextualMonth(amountStr)) {
                parts[4] = "0";
            } else {
                try {
                    Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    parts[4] = "0";
                }
            }
        }

        // Исправление показаний
        for (int i = 5; i < parts.length; i += 2) {
            String reading = (i + 1 < parts.length) ? parts[i + 1].trim() : "";
            if (!reading.isEmpty()) {
                if (isTextualMonth(reading)) {
                    parts[i + 1] = "0";
                } else if (!isValidMeterOrReading(reading)) {
                    parts[i + 1] = "0";
                }
            }
        }

        // Собираем исправленную строку
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(";").append(parts[i]);
        }

        return result.toString();
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
     * Проверяет, является ли значение допустимым
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
