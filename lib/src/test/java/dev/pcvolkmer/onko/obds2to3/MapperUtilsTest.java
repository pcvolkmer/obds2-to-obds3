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

import de.basisdatensatz.obds.v2.ADTGEKID;
import de.basisdatensatz.obds.v2.JNUTyp;
import de.basisdatensatz.obds.v3.DatumTagOderMonatGenauTyp;
import de.basisdatensatz.obds.v3.DatumTagOderMonatOderJahrOderNichtGenauTyp;
import de.basisdatensatz.obds.v3.JNU;
import de.basisdatensatz.obds.v3.TodTyp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MapperUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "01.01.2024,2024-01-01,E",
            "04.02.1978,1978-02-04,E",
            "00.10.2024,2024-10-01,T",
            "00.00.2024,2024-01-01,M",
            "00.00.0000,1900-01-01,V",
    })
    void shouldMapDate(String obdsv2DateString, String expectedDateString, String expectedPrecision) {
        var actual = MapperUtils.mapDateString(obdsv2DateString);
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue().toXMLFormat()).startsWith(expectedDateString);
        assertThat(actual.get().getDatumsgenauigkeit()).isEqualTo(DatumTagOderMonatOderJahrOderNichtGenauTyp.DatumsgenauigkeitTagOderMonatOderJahrOderNichtGenau.fromValue(expectedPrecision));
    }

    @Test
    void shouldNotMapDate() {
        var actual = MapperUtils.mapDateString("somethingbad");
        assertThat(actual).isNotPresent();
    }

    @ParameterizedTest
    @CsvSource({
            "01.01.2024,2024-01-01,E",
            "04.02.1978,1978-02-04,E",
            "00.10.2024,2024-10-01,T",
            "00.00.2020,2020-01-01,T",
    })
    void shouldMapDateGenau(String obdsv2DateString, String expectedDateString, String expectedPrecision) {
        var actual = MapperUtils.mapDateStringGenau(obdsv2DateString);
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue().toXMLFormat()).startsWith(expectedDateString);
        assertThat(actual.get().getDatumsgenauigkeit()).isEqualTo(DatumTagOderMonatGenauTyp.DatumsgenauigkeitTagOderMonatGenau.fromValue(expectedPrecision));
    }

    @Test
    void shouldNotMapDateGenau() {
        var actual = MapperUtils.mapDateStringGenau("somethingbad");
        assertThat(actual).isNotPresent();
    }

    @ParameterizedTest
    @CsvSource({
            "01.01.2024,2024-01-01",
            "04.02.1978,1978-02-04",
            "00.10.2024,2024-10-01",
            "00.00.2024,2024-01-01",
    })
    void shouldMapToLocalDate(String obdsv2DateString, String expectedDateString) {
        var actual = MapperUtils.mapDateString(obdsv2DateString);
        assertThat(actual).isPresent();
        assertThat(actual.get().getValue().toString()).startsWith(expectedDateString);
    }

    @Test
    void shouldRemoveAllInvalidChars() {
        var actual = MapperUtils.trimToMatchDatatype("datatypeBtrimmed", "Das ist ein パウルTest");
        assertThat(actual).isPresent().isEqualTo(Optional.of("Das ist ein Test"));
    }

    @ParameterizedTest
    @MethodSource("nullMappingSource")
    void shouldOnlyMapValueIfNotNull(JNUTyp given, JNU expected) {
        var givenTod = new ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeVerlauf.Verlauf.Tod();
        givenTod.setTodTumorbedingt(given);
        var actualTod = new TodTyp();

        MapperUtils.ifNotNull(givenTod.getTodTumorbedingt(), value ->
                actualTod.setTodTumorbedingt(JNU.fromValue(value.value()))
        );
        assertThat(actualTod.getTodTumorbedingt()).isEqualTo(expected);
    }

    public static Stream<Arguments> nullMappingSource() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(JNUTyp.J, JNU.J),
                Arguments.of(JNUTyp.N, JNU.N),
                Arguments.of(JNUTyp.U, JNU.U)
        );
    }

}
