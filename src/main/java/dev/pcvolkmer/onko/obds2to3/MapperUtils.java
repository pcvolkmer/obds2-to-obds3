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
import de.basisdatensatz.obds.v3.DatumTagOderMonatOderJahrOderNichtGenauTyp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.regex.Pattern;

class MapperUtils {

    private MapperUtils() {}

    /** Wandelt oBDS v2 Datumsstring in oBDS v3 "nicht genau" Datum um.
     *
     * @link <a href="https://plattform65c.atlassian.net/wiki/spaces/UMK/pages/15532303/Datum_Tag_oder_Monat_oder_Jahr_oder_nicht_genau_Typ">Spezifikation</a>
     * @param date
     * @return
     */
    public static Optional<DatumTagOderMonatOderJahrOderNichtGenauTyp> mapDateString(String date) {
        var result = new DatumTagOderMonatOderJahrOderNichtGenauTyp();

        var obdsV2datePattern = Pattern.compile("(?<day>([0-2]\\d)|(3[01]))\\.(?<month>(0\\d)|(1[0-2]))\\.(?<year>(18|19|20)\\d\\d)");
        var matcher = obdsV2datePattern.matcher(date);
        if (matcher.matches()) {
            var day = Integer.parseInt(matcher.group("day"));
            var month = Integer.parseInt(matcher.group("month"));
            var year = Integer.parseInt(matcher.group("year"));

            var calendar = new GregorianCalendar();
            calendar.clear();

            calendar.set(Calendar.YEAR, year);
            result.setDatumsgenauigkeit("M");

            if (month > 0) {
                // Starts with month "0"
                calendar.set(Calendar.MONTH, month - 1);
                result.setDatumsgenauigkeit("T");
            }

            if (day > 0) {
                calendar.set(Calendar.DAY_OF_MONTH, day);
                result.setDatumsgenauigkeit("E");
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

    /** Wandelt oBDS v2 Datumsstring in oBDS v3 "genau" Datum um.
     *
     * @link <a href="https://plattform65c.atlassian.net/wiki/spaces/UMK/pages/15532315/Datum_Tag_oder_Monat_genau_Typ">Spezifikation</a>
     * @param date
     * @return
     */
    public static Optional<DatumTagOderMonatGenauTyp> mapDateStringGenau(String date) {
        var result = new DatumTagOderMonatGenauTyp();

        var obdsV2datePattern = Pattern.compile("(?<day>([0-2]\\d)|(3[01]))\\.(?<month>(0\\d)|(1[0-2]))\\.(?<year>(18|19|20)\\d\\d)");
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
                result.setDatumsgenauigkeit("T");
            }

            if (day > 0) {
                calendar.set(Calendar.DAY_OF_MONTH, day);
                result.setDatumsgenauigkeit("E");
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

    public static Optional<LocalDate> parseDate(String dateString) {
        if (null == dateString || dateString.isEmpty()) {
            return Optional.empty();
        }

        var obdsV2datePattern = Pattern.compile("(?<day>([0-2]\\d)|(3[01]))\\.(?<month>(0\\d)|(1[0-2]))\\.(?<year>(18|19|20)\\d\\d)");
        var matcher = obdsV2datePattern.matcher(dateString);
        if (matcher.matches()) {
            var day = Integer.parseInt(matcher.group("day"));
            var month = Integer.parseInt(matcher.group("month"));
            var year = Integer.parseInt(matcher.group("year"));

            return Optional.of(LocalDate.of(year, month, day));
        }
        return Optional.empty();
    }
}
