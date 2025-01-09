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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ObdsMapperTest {

    private ObdsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObdsMapper();
    }

    @ParameterizedTest
    @CsvSource({
            "testdaten/obdsv2_1.xml,testdaten/obdsv3_1.xml",
            "testdaten/obdsv2_gueltige-adresse.xml,testdaten/obdsv3_1.xml",
            "testdaten/obdsv2_verlauf.xml,testdaten/obdsv3_verlauf.xml",
            "testdaten/obdsv2_tumorkonferenz.xml,testdaten/obdsv3_tumorkonferenz.xml",
            "testdaten/obdsv2_modul_prostata.xml,testdaten/obdsv3_modul_prostata.xml",
            "testdaten/obdsv2_zusatzitems.xml,testdaten/obdsv3_zusatzitems.xml",
            "testdaten/obdsv2_histologie.xml,testdaten/obdsv3_histologie.xml",
            "testdaten/obdsv2_ohne-adresse.xml,testdaten/obdsv3_ohne-adresse.xml",
    })
    void shouldMapObdsFile(String obdsV2File, String obdsV3File) throws Exception {
        var obdsV2String = new String(getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());
        var obdsV3String = new String(getClass().getClassLoader().getResource(obdsV3File).openStream().readAllBytes());

        var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
        assertThat(obdsv2).isNotNull();

        assertThat(mapper.writeMappedXmlString(obdsv2)).isEqualTo(obdsV3String);
    }

}
