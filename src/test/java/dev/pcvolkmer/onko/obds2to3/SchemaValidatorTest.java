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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "testdaten/obdsv2_1.xml",
            "testdaten/obdsv2_gueltige-adresse.xml",
            "testdaten/obdsv2_verlauf.xml"
    })
    void shouldValidateCorrectV2Testdata(String filename) throws Exception {
        var xmlString = new String(getClass().getClassLoader().getResource(filename).openStream().readAllBytes());
        assertThat(SchemaValidator.isValid(xmlString, SchemaValidator.SchemaVersion.ADT_GEKID_2_2_3)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "testdaten/obdsv3_1.xml",
            "testdaten/obdsv3_verlauf.xml"
    })
    void shouldValidateCorrectV3Testdata(String filename) throws Exception {
        var xmlString = new String(getClass().getClassLoader().getResource(filename).openStream().readAllBytes());
        assertThat(SchemaValidator.isValid(xmlString, SchemaValidator.SchemaVersion.OBDS_3_0_3)).isTrue();
    }

}