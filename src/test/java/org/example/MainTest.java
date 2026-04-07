package org.example;

import org.example.model.common.ParseResult;
import org.example.service.parsing.RecordParser;
import org.example.service.parsing.RecordCorrector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты парсинга и исправления записей
 */
@DisplayName("Тесты парсинга и исправления записей")
class MainTest {

    private final RecordParser parser = new RecordParser();
    private final RecordCorrector corrector = new RecordCorrector();

    @Test
    @DisplayName("Парсинг валидной записи")
    void testParseValidRecord() {
        String line = "900045964;Н********* *** **********;Лопатницы с., Главная, 63, 0;519;228.47;3302461115 ХВС;185.0000;";
        
        ParseResult result = parser.parse(line);
        
        assertTrue(result.isValid(), "Запись должна быть валидной");
    }

    @Test
    @DisplayName("Парсинг записи с пустым ФИО")
    void testParseRecordWithEmptyFio() {
        String line = "900046144;.*  ;Барское Городище с., Раковицы, 9, 0;519;0.01";
        
        ParseResult result = parser.parse(line);
        
        assertFalse(result.isValid(), "Запись с маскированным ФИО должна быть невалидной");
        assertTrue(result.getErrorMessage().contains("Пустое ФИО"), "Ошибка должна указывать на пустое ФИО");
    }

    @Test
    @DisplayName("Парсинг записи с адресом в поле ФИО")
    void testParseRecordWithAddressInFioField() {
        String line = "800048789;Ковров г, Свердлова ул, 15, 48;519;268.0000;3301795066 счетчик ХВ к;22.0000";
        
        ParseResult result = parser.parse(line);
        
        assertFalse(result.isValid(), "Запись с адресом в поле ФИО должна быть невалидной");
    }

    @Test
    @DisplayName("Исправление записи с пустым ФИО (.*)")
    void testCorrectRecordWithDotAsteriskFio() {
        String line = "900046144;.*  ;Барское Городище с., Раковицы, 9, 0;519;0.01";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
        assertTrue(corrected.startsWith("900046144;данные отсутствуют;"), "ФИО должно быть на правильном месте");
    }

    @Test
    @DisplayName("Исправление записи с адресом в поле ФИО")
    void testCorrectRecordWithAddressInFioField() {
        String line = "800048789;Ковров г, Свердлова ул, 15, 48;519;268.0000;3301795066 счетчик ХВ к;22.0000";
        String errorMessage = "Неполный адрес (меньше 3 запятых)";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
        assertTrue(corrected.contains("Ковров г, Свердлова ул, 15, 48"), "Адрес должен сохраниться");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '1 * *'")
    void testCorrectRecordWithDigitAsteriskFio() {
        String line = "900074154;1 * *;Кольчугино г, Пос Труда, 7, 65;519;352.28";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '. * *'")
    void testCorrectRecordWithDotAsteriskSpaceFio() {
        String line = "900013143;. * *;Кольчугино г, Московская ул, 66, 79;519;1667.63;3301619338 ХВС;32.0000";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '. *'")
    void testCorrectRecordWithDotSpaceAsteriskFio() {
        String line = "600003728;. *;Камешково г, Смурова ул, 11, 44;519;0.01;3302546265 ХВС;3.0000";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '.'")
    void testCorrectRecordWithSingleDotFio() {
        String line = "330247704;.;Владимир г, Юбилейная ул, 24, 5;519;345.15";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '2**'")
    void testCorrectRecordWithDigitAsterisksFio() {
        String line = "170010956;2**;Суздаль г, Михайловская ул, 82Б, 3;519;159.19";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '.**  '")
    void testCorrectRecordWithDotAsterisksSpacesFio() {
        String line = "900095569;.**  ;Лакинск г, Ленина пр-кт, 25, 12;519;0.01";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '5 ******* ************'")
    void testCorrectRecordWithDigitSpacesAsterisksFio() {
        String line = "900115895;5 ******* ************;Вышманово д., Вышмановская ул, 19 В, 0;519;729.63";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '3********* *********** ** ** *'")
    void testCorrectRecordWithComplexMaskedFio() {
        String line = "330047137;3********* *********** ** ** *;Владимир г, Красноармейская ул, 43, 64;519;1044.17";
        String errorMessage = "Пустое ФИО плательщика";
        
        String corrected = corrector.correct(line, errorMessage);
        
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление адреса с 2 запятыми (населенный пункт, улица, дом)")
    void testCorrectAddressWithTwoCommasLocalityStreetHouse() {
        String line = "900046650;П******* ***** *********;Сокол п, 7, 4;519;5938.56;3301510692 ГВС;362.8000;";
        String errorMessage = "Неполный адрес (меньше 3 запятых)";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("Улица не указана"), "Должна быть добавлена 'Улица не указана'");
    }

    @Test
    @DisplayName("Исправление адреса с 2 запятыми (населенный пункт, дом, квартира)")
    void testCorrectAddressWithTwoCommasLocalityHouseApartment() {
        String line = "900046657;Б************ **** ;Сокол п, 8, 37;519;0.01";
        String errorMessage = "Неполный адрес (меньше 3 запятых)";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("Улица не указана"), "Должна быть добавлена 'Улица не указана'");
    }

    @Test
    @DisplayName("Исправление адреса с 2 запятыми (населенный пункт, улица)")
    void testCorrectAddressWithTwoCommasLocalityStreet() {
        String line = "900046723;Л******* **** ;Сокол п, 6, 3;519;3149.53;3301510645 ГВС;181.6000";
        String errorMessage = "Неполный адрес (меньше 3 запятых)";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("Улица не указана") || corrected.contains("Квартира не указана"),
            "Должны быть добавлены недостающие части адреса");
    }

    @Test
    @DisplayName("Исправление периода с текстовым месяцем")
    void testCorrectPeriodWithTextualMonth() {
        String line = "900046509;Я****** ********* ************;Омутское с., Центральная, 16, 1;сен.74;3302420241 СХВ;12.0000";
        String errorMessage = "Период в текстовом формате";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0;"), "Период должен быть заменён на 0");
    }

    @Test
    @DisplayName("Исправление суммы с текстовым месяцем")
    void testCorrectAmountWithTextualMonth() {
        String line = "900046509;Я****** ********* ************;Омутское с., Центральная, 16, 1;519;сен.74;3302420241 СХВ;12.0000";
        String errorMessage = "Неверный формат суммы начисления";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0;") || corrected.contains(";;"), "Сумма должна быть заменена на 0");
    }

    @Test
    @DisplayName("Исправление показаний с текстовым месяцем")
    void testCorrectReadingWithTextualMonth() {
        String line = "900047861;М******* **** ;Торчино с., Никольская, 43, 0;519;0.01;3302468447 ХВС;май.00";
        String errorMessage = "Текстовый месяц в поле показаний";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0") || corrected.endsWith("0") || corrected.endsWith("0;"), "Показания должны быть заменены на 0");
    }

    @Test
    @DisplayName("Исправление записи с недостаточным количеством полей")
    void testCorrectRecordWithInsufficientFields() {
        String line = "330247291;Владимир г, Юбилейная ул, 18, 48;519;346.45";
        String errorMessage = "Недостаточно полей в строке";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.split(";").length >= 5, "Должно быть не менее 5 полей");
        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с пустым ФИО (только пробелы)")
    void testCorrectRecordWithSpacesOnlyFio() {
        String line = "900052681;  ;Гусь-Хрустальный г, Интернациональная ул, 5, 9;519;0.01";
        String errorMessage = "Пустое ФИО плательщика";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с адресом более 5 запятых")
    void testCorrectAddressWithMoreThan5Commas() {
        String line = "900059827;Б****** ********* ********;Гусь-Хрустальный г, Перегрузочная ул, 5а, 80,81,82,83;519;1288.34";
        String errorMessage = "Подозрительный адрес (больше 5 запятых)";

        String corrected = corrector.correct(line, errorMessage);

        // Квартиры должны быть объединены
        int commaCount = corrected.length() - corrected.replace(",", "").length();
        assertTrue(commaCount <= 5, "Запятых должно быть не более 5");
    }

    @Test
    @DisplayName("Валидная запись не должна изменяться при исправлении")
    void testValidRecordNotChanged() {
        String line = "900045964;Н********* *** **********;Лопатницы с., Главная, 63, 0;519;228.47;3302461115 ХВС;185.0000";
        String errorMessage = "";

        String corrected = corrector.correct(line, errorMessage);

        assertEquals(line, corrected, "Валидная запись не должна изменяться");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '4 * *'")
    void testCorrectRecordWithFourAsteriskAsteriskFio() {
        String line = "900008061;4 * *;Кольчугино г, Инициативная ул, 19, 21;519;4335.52;3301622353 ГВС;178.0000";
        String errorMessage = "Пустое ФИО плательщика";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '.** *** ***'")
    void testCorrectRecordWithDotAsterisksMultipleFio() {
        String line = "900008422;.** *** ***;Кольчугино г, Коллективная ул, 35, 107;519;1362.22";
        String errorMessage = "Пустое ФИО плательщика";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Исправление записи с ФИО '1*********'")
    void testCorrectRecordWithDigitAsterisksLongFio() {
        String line = "140014809;1 ***********;Ставрово п, Южная ул, 2, 35;519;2659.17;3301989489 ХВС;274.0000";
        String errorMessage = "Пустое ФИО плательщика";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains("данные отсутствуют"), "ФИО должно быть заменено на 'данные отсутствуют'");
    }

    @Test
    @DisplayName("Парсинг записи с пустым номером ЛС")
    void testParseRecordWithEmptyAccountNumber() {
        String line = ";Иванов И.И.;Город, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с пустым номером ЛС должна быть невалидной");
    }

    @Test
    @DisplayName("Парсинг записи с нечисловым номером ЛС")
    void testParseRecordWithNonNumericAccountNumber() {
        String line = "ABC123;Иванов И.И.;Город, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с нечисловым номером ЛС должна быть невалидной");
    }

    @Test
    @DisplayName("Парсинг записи с пустым адресом")
    void testParseRecordWithEmptyAddress() {
        String line = "900045964;Иванов И.И.;;519;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с пустым адресом должна быть невалидной");
    }

    @Test
    @DisplayName("Парсинг записи с адресом без запятых")
    void testParseRecordWithAddressNoCommas() {
        String line = "900045964;Иванов И.И.;Город;519;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с адресом без запятых должна быть невалидной");
    }

    @Test
    @DisplayName("Парсинг записи с пустым периодом")
    void testParseRecordWithEmptyPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с пустым периодом должна быть невалидной");
    }

    @Test
    @DisplayName("Исправление записи с пустым периодом")
    void testCorrectRecordWithEmptyPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;;100.00";
        String errorMessage = "Пустой период начисления";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0;"), "Период должен быть заменён на 0");
    }

    @Test
    @DisplayName("Парсинг записи с периодом в текстовом формате")
    void testParseRecordWithTextualPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;янв.24;100.00";

        ParseResult result = parser.parse(line);
        String errorMessage = result.getErrorMessage();

        assertFalse(result.isValid(), "Запись с текстовым месяцем в периоде должна быть невалидной");
        assertTrue(errorMessage.contains("Период в текстовом формате") || errorMessage.contains("текстов"), "Ошибка должна указывать на текстовый формат");
    }

    @Test
    @DisplayName("Исправление записи с периодом в текстовом формате")
    void testCorrectRecordWithTextualPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;янв.24;100.00";
        String errorMessage = "Период в текстовом формате";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0;"), "Период должен быть заменён на 0");
    }

    @Test
    @DisplayName("Парсинг записи с пустой суммой")
    void testParseRecordWithEmptyAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с пустой суммой должна быть невалидной");
    }

    @Test
    @DisplayName("Исправление записи с пустой суммой")
    void testCorrectRecordWithEmptyAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;";
        String errorMessage = "Неверный формат суммы начисления";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0") || corrected.endsWith("0"), "Сумма должна быть заменена на 0");
    }

    @Test
    @DisplayName("Парсинг записи с несколькими приборами учета")
    void testParseRecordWithMultipleMeters() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.0000;3302461116 ГВС;100.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с несколькими приборами должна быть валидной");
    }

    @Test
    @DisplayName("Парсинг записи с отрицательной суммой (перерасчет)")
    void testParseRecordWithNegativeAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;-100.50";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с отрицательной суммой (перерасчет) должна быть валидной");
    }

    @Test
    @DisplayName("Исправление записи с текстовым месяцем в показаниях")
    void testCorrectReadingWithTextualMonthInMeterReading() {
        String line = "900047861;М******* **** ;Торчино с., Никольская, 43, 0;519;0.01;3302468447 ХВС;май.00";
        String errorMessage = "Текстовый месяц в поле показаний";

        String corrected = corrector.correct(line, errorMessage);

        assertTrue(corrected.contains(";0") || corrected.endsWith("0") || corrected.endsWith("0;"), "Показания должны быть заменены на 0");
    }

    @Test
    @DisplayName("Валидный адрес с 3 запятыми")
    void testValidAddressWithThreeCommas() {
        String line = "900045964;Иванов И.И.;Город, Улица, Дом, Квартира;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Адрес с 3 запятыми должен быть валидным");
    }

    @Test
    @DisplayName("Адрес с 4 запятыми (5 частей)")
    void testAddressWithFourCommas() {
        String line = "900059827;Б****** ********* ********;Гусь-Хрустальный г, Перегрузочная ул, 5а, 80,81,82,83;519;1288.34";
        String errorMessage = "Подозрительный адрес (больше 5 запятых)";

        String corrected = corrector.correct(line, errorMessage);

        // Квартиры должны быть объединены
        int commaCount = corrected.length() - corrected.replace(",", "").length();
        assertTrue(commaCount <= 5, "Запятых должно быть не более 5 после исправления");
    }

    @Test
    @DisplayName("Адрес с пустыми частями")
    void testAddressWithEmptyParts() {
        String line = "900076103;Ш****** ******* **********;Беречино, 182, ,;519;59.69";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Адрес с пустыми частями должен быть невалидным");
    }

    @Test
    @DisplayName("Адрес со спецсимволами (скобки)")
    void testAddressWithSpecialChars() {
        String line = "800071344;Г** ********* **********;Ковров г, Ковров-8 тер, 2 общежитие (инв 332), 1;519;171.72";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Адрес со скобками должен быть валидным");
    }

    @Test
    @DisplayName("Адрес с дефисом в номере дома")
    void testAddressWithDashInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1-2, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с дефисом в номере дома должна быть валидной");
    }

    @Test
    @DisplayName("Запись без приборов учёта")
    void testRecordWithoutMeters() {
        String line = "900046144;.*  ;Барское Городище с., Раковицы, 9, 0;519;0.01";

        ParseResult result = parser.parse(line);

        // Запись невалидна из-за ФИО, но парсер должен обработать
        assertNotNull(result, "Парсер должен обработать запись без приборов");
    }

    @Test
    @DisplayName("Запись с одним прибором учёта")
    void testRecordWithSingleMeter() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с одним прибором должна быть валидной");
    }

    @Test
    @DisplayName("Запись с двумя приборами учёта")
    void testRecordWithTwoMeters() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.0000;3302461116 ГВС;100.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с двумя приборами должна быть валидной");
    }

    @Test
    @DisplayName("Запись с тремя приборами учёта")
    void testRecordWithThreeMeters() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.0000;3302461116 ГВС;100.0000;3302461117 Эл-во;50.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с тремя приборами должна быть валидной");
    }

    @Test
    @DisplayName("Запись с показаниями 0")
    void testRecordWithZeroReadings() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;0";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с нулевыми показаниями должна быть валидной");
    }

    @Test
    @DisplayName("Запись с дробными показаниями")
    void testRecordWithDecimalReadings() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.1234";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с дробными показаниями должна быть валидной");
    }

    @Test
    @DisplayName("Запись с нечисловыми показаниями")
    void testRecordWithNonNumericReadings() {
        String line = "900047861;М******* **** ;Торчино с., Никольская, 43, 0;519;0.01;3302468447 ХВС;май.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Запись с нечисловыми показаниями должна быть невалидной");
    }

    @Test
    @DisplayName("Запись с пустым названием прибора")
    void testRecordWithEmptyMeterName() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;;185.0000";

        ParseResult result = parser.parse(line);

        // Парсер должен обработать, даже если прибор пустой
        assertNotNull(result, "Парсер должен обработать запись с пустым прибором");
    }

    @Test
    @DisplayName("Валидация номера прибора с буквами и цифрами")
    void testValidateMeterWithLettersAndDigits() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;3302461115 ХВС;185.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Номер прибора с буквами и цифрами должен быть валидным");
    }

    @Test
    @DisplayName("Валидация номера прибора со спецсимволами")
    void testValidateMeterWithSpecialChars() {
        String line = "900028284;П***** ******* **********;Красная Горбатка п, Северная ул, 36, 0;519;347.76;3302403504 svost-3@inbo;78.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Номер прибора со спецсимволами (@, -) должен быть валидным");
    }

    @Test
    @DisplayName("Валидация текстового месяца (янв.24)")
    void testValidateTextualMonthJan() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;янв.24;100.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Период с текстовым месяцем должен быть невалидным");
    }

    @Test
    @DisplayName("Валидация текстового месяца (май.00)")
    void testValidateTextualMonthMay() {
        String line = "900047861;М******* **** ;Торчино с., Никольская, 43, 0;519;0.01;3302468447 ХВС;май.00";

        ParseResult result = parser.parse(line);

        assertFalse(result.isValid(), "Показания с текстовым месяцем должны быть невалидными");
    }

    @Test
    @DisplayName("Запись с очень длинным ФИО")
    void testRecordWithVeryLongFio() {
        String line = "900045964;Иванов Иван Иванович Петрович Сидорович-Козловский;Город, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с длинным ФИО должна быть валидной");
    }

    @Test
    @DisplayName("Запись с очень длинным названием прибора")
    void testRecordWithVeryLongMeterName() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00;Очень длинное название прибора учёта холодной воды с дополнительными символами 12345;185.0000";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с длинным названием прибора должна быть валидной");
    }

    @Test
    @DisplayName("Запись с суммой 0")
    void testRecordWithZeroAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;0";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с нулевой суммой должна быть валидной");
    }

    @Test
    @DisplayName("Запись с очень большой суммой")
    void testRecordWithVeryLargeAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;999999999.99";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с большой суммой должна быть валидной");
    }

    @Test
    @DisplayName("Запись с отрицательной суммой (перерасчет)")
    void testRecordWithNegativeAmount() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;-100.50";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с отрицательной суммой (перерасчет) должна быть валидной");
    }

    @Test
    @DisplayName("Запись с квартирой '0'")
    void testRecordWithApartmentZero() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 0;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с квартирой '0' должна быть валидной");
    }

    @Test
    @DisplayName("Запись с несколькими квартирами через пробел")
    void testRecordWithMultipleApartments() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1 2 3;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с несколькими квартирами должна быть валидной");
    }

    @Test
    @DisplayName("Запись с буквой в номере дома")
    void testRecordWithLetterInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1А, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с буквой в номере дома должна быть валидной");
    }

    @Test
    @DisplayName("Запись с дефисом в номере дома")
    void testRecordWithDashInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1-2, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с дефисом в номере дома должна быть валидной");
    }

    @Test
    @DisplayName("Запись с точкой в номере дома")
    void testRecordWithDotInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1.1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с точкой в номере дома должна быть валидной");
    }

    @Test
    @DisplayName("Запись с периодом из 2 цифр")
    void testRecordWithTwoDigitPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;12;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с 2-значным периодом должна быть валидной");
    }

    @Test
    @DisplayName("Запись с периодом из 4 цифр")
    void testRecordWithFourDigitPeriod() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;2024;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с 4-значным периодом должна быть валидной");
    }

    @Test
    @DisplayName("Запись с названием населенного пункта 'с' (село)")
    void testRecordWithVillageLocality() {
        String line = "900045964;Иванов И.И.;Село, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с селом должна быть валидной");
    }

    @Test
    @DisplayName("Запись с названием населенного пункта 'п' (поселок)")
    void testRecordWithSettlementLocality() {
        String line = "900045964;Иванов И.И.;Поселок, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с поселком должна быть валидной");
    }

    @Test
    @DisplayName("Запись с названием населенного пункта 'г' (город)")
    void testRecordWithCityLocality() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с городом должна быть валидной");
    }

    @Test
    @DisplayName("Запись с названием населенного пункта 'дер' (деревня)")
    void testRecordWithVillageShortLocality() {
        String line = "900045964;Иванов И.И.;Деревня, Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с деревней должна быть валидной");
    }

    @Test
    @DisplayName("Запись с р-н в названии")
    void testRecordWithDistrictInName() {
        String line = "900045964;Иванов И.И.;Город (Ковровский р-н), Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с р-н должна быть валидной");
    }

    @Test
    @DisplayName("Запись с мкр в названии")
    void testRecordWithMicrodistrictInName() {
        String line = "900045964;Иванов И.И.;Город (мкр Центральный), Улица, 1, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с мкр должна быть валидной");
    }

    @Test
    @DisplayName("Запись с корп. в номере дома")
    void testRecordWithBuildingInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1 корп. 2, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись с корп. должна быть валидной");
    }

    @Test
    @DisplayName("Запись с стр. в номере дома")
    void testRecordWithStructureInHouseNumber() {
        String line = "900045964;Иванов И.И.;Город, Улица, 1 стр. 2, 1;519;100.00";

        ParseResult result = parser.parse(line);

        assertTrue(result.isValid(), "Запись со стр. должна быть валидной");
    }
}
