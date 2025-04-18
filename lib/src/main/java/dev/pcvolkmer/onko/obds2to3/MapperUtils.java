/*
 * This file is part of obds2-to-obds3
 *
 * Copyright (c) 2024 the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.pcvolkmer.onko.obds2to3;

import de.basisdatensatz.obds.v3.DatumTagOderMonatGenauTyp;
import de.basisdatensatz.obds.v3.DatumTagOderMonatGenauTypSchaetzOptional;
import de.basisdatensatz.obds.v3.DatumTagOderMonatOderJahrOderNichtGenauTyp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.regex.Pattern;

class MapperUtils {

    public static final String OBDS2_DATE_REGEX = "(?<day>([0-2]\\d)|(3[01]))\\.(?<month>(0\\d)|(1[0-2]))\\.(?<year>(18|19|20)\\d\\d)";

    private MapperUtils() {}


    /**
     * Wandelt oBDS v2 Datumsstring in oBDS v3 "nicht genau" Datum um.
     *
     * @param date
     * @return
     * @link <a href="https://plattform65c.atlassian.net/wiki/spaces/UMK/pages/15532303/Datum_Tag_oder_Monat_oder_Jahr_oder_nicht_genau_Typ">Spezifikation</a>
     */
    public static Optional<DatumTagOderMonatOderJahrOderNichtGenauTyp> mapDateString(String date) {
        if (null == date) {
            return Optional.empty();
        }

        var result = new DatumTagOderMonatOderJahrOderNichtGenauTyp();

        var obdsV2datePattern = Pattern.compile(OBDS2_DATE_REGEX);
        var matcher = obdsV2datePattern.matcher(date);
        if (matcher.matches()) {
            var day = Integer.parseInt(matcher.group("day"));
            var month = Integer.parseInt(matcher.group("month"));
            var year = Integer.parseInt(matcher.group("year"));

            var calendar = new GregorianCalendar();
            calendar.clear();

            calendar.set(Calendar.YEAR, year);
            result.setDatumsgenauigkeit(DatumTagOderMonatOderJahrOderNichtGenauTyp.DatumsgenauigkeitTagOderMonatOderJahrOderNichtGenau.M);

            if (month > 0) {
                // Starts with month "0"
                calendar.set(Calendar.MONTH, month - 1);
                result.setDatumsgenauigkeit(DatumTagOderMonatOderJahrOderNichtGenauTyp.DatumsgenauigkeitTagOderMonatOderJahrOderNichtGenau.T);
            }

            if (day > 0) {
                calendar.set(Calendar.DAY_OF_MONTH, day);
                result.setDatumsgenauigkeit(DatumTagOderMonatOderJahrOderNichtGenauTyp.DatumsgenauigkeitTagOderMonatOderJahrOderNichtGenau.E);
            }

            try {
                result.setValue(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
                return Optional.of(result);
            } catch (DatatypeConfigurationException e) {
                return Optional.empty();
            }
        }

        // Schätze kein Datum (Genauigkeit "V")
        return Optional.empty();
    }

    /**
     * Wandelt oBDS v2 Datumsstring in oBDS v3 "genau" Datum um.
     *
     * @param date
     * @return
     * @link <a href="https://plattform65c.atlassian.net/wiki/spaces/UMK/pages/15532315/Datum_Tag_oder_Monat_genau_Typ">Spezifikation</a>
     */
    public static Optional<DatumTagOderMonatGenauTyp> mapDateStringGenau(String date) {
        if (null == date) {
            return Optional.empty();
        }

        var result = new DatumTagOderMonatGenauTyp();

        var obdsV2datePattern = Pattern.compile(OBDS2_DATE_REGEX);
        var matcher = obdsV2datePattern.matcher(date);
        if (matcher.matches()) {
            var day = Integer.parseInt(matcher.group("day"));
            var month = Integer.parseInt(matcher.group("month"));
            var year = Integer.parseInt(matcher.group("year"));

            var calendar = new GregorianCalendar();
            calendar.clear();

            calendar.set(Calendar.YEAR, year);

            if (month > 0) {
                // Starts with month "0"
                calendar.set(Calendar.MONTH, month - 1);
                result.setDatumsgenauigkeit(DatumTagOderMonatGenauTyp.DatumsgenauigkeitTagOderMonatGenau.T);
            }

            if (day > 0) {
                calendar.set(Calendar.DAY_OF_MONTH, day);
                result.setDatumsgenauigkeit(DatumTagOderMonatGenauTyp.DatumsgenauigkeitTagOderMonatGenau.E);
            }

            try {
                result.setValue(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
                return Optional.of(result);
            } catch (DatatypeConfigurationException e) {
                return Optional.empty();
            }
        }

        // Schätze kein Datum (Genauigkeit "V")
        return Optional.empty();
    }

    /**
     * Wandelt XMLGregorianCalendar in oBDS v3 "schaetz" Datum mit optionaler Genauigkeitsangabe um.
     *
     * @param calendar
     * @return
     */
    public static Optional<DatumTagOderMonatGenauTypSchaetzOptional> mapDatumTagOderMonatGenauTypSchaetzOptional(XMLGregorianCalendar calendar) {
        if (null == calendar) {
            return Optional.empty();
        }

        var result = new DatumTagOderMonatGenauTypSchaetzOptional();
        result.setValue(calendar);

        if (calendar.getMonth() > 0) {
            result.setDatumsgenauigkeit(DatumTagOderMonatGenauTypSchaetzOptional.DatumsgenauigkeitTagOderMonatGenauTypSchaetzOptional.T);
        }

        if (calendar.getDay() > 0) {
            result.setDatumsgenauigkeit(DatumTagOderMonatGenauTypSchaetzOptional.DatumsgenauigkeitTagOderMonatGenauTypSchaetzOptional.T);
        }

        return Optional.of(result);
    }

    public static Optional<LocalDate> parseDate(String dateString) {
        if (null == dateString || dateString.trim().isEmpty()) {
            return Optional.empty();
        }

        var obdsV2datePattern = Pattern.compile(OBDS2_DATE_REGEX);
        var matcher = obdsV2datePattern.matcher(dateString.trim());
        if (matcher.matches()) {
            var day = Integer.parseInt(matcher.group("day"));
            var month = Integer.parseInt(matcher.group("month"));
            var year = Integer.parseInt(matcher.group("year"));

            return Optional.of(LocalDate.of(year, month, day));
        }
        return Optional.empty();
    }

    /**
     * Entferne alle nicht erlaubten Zeichen aus String
     *
     * @param datatypeName Name des Datentyps gemäß oBDS 3 Schema
     * @param string       Eingabestring
     * @return String mit entfernten unerlaubten Zeichen oder empty Optional
     */
    public static Optional<String> trimToMatchDatatype(String datatypeName, String string) {
        return SchemaValidator
                .regexpPattern(datatypeName, SchemaValidator.SchemaVersion.OBDS_3_0_4)
                .map(pattern -> string.replaceAll(String.format("[^%s]", pattern.pattern()), ""));
    }
}
