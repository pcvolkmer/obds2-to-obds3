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

package io.github.bzkf.obds2toobds3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.basisdatensatz.obds.v2.ADTGEKID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ObdsMapperTest {

  @Nested
  class DefaultObdsMapperTest {

    private ObdsMapper mapper;

    @BeforeEach
    void setUp() {
      mapper = ObdsMapper.builder().build();
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
      "testdaten/obdsv2_keine-fruehere-namen.xml,testdaten/obdsv3_keine-fruehere-namen.xml",
      "testdaten/obdsv2_diagnose-zu-tumorzuordung.xml,testdaten/obdsv3_diagnose-zu-tumorzuordung.xml",
      "testdaten/obdsv2_kv-ersatzcode.xml,testdaten/obdsv3_kv-ersatzcode.xml",
      "testdaten/obdsv2_pkv.xml,testdaten/obdsv3_pkv.xml",
      "testdaten/obdsv2_keine-diagnose.xml,testdaten/obdsv3_keine-diagnose.xml",
      "testdaten/obdsv2_op_1.xml,testdaten/obdsv3_op_1.xml",
      "testdaten/obdsv2_st_1.xml,testdaten/obdsv3_st_1.xml",
      "testdaten/obdsv2_syst_1.xml,testdaten/obdsv3_syst_1.xml",
      "testdaten/obdsv2_verlauf_histologie.xml,testdaten/obdsv3_verlauf_histologie.xml"
    })
    void shouldMapObdsFile(String obdsV2File, String obdsV3File) throws Exception {
      var obdsV2String =
          new String(
              getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());
      var obdsV3String =
          new String(
              getClass().getClassLoader().getResource(obdsV3File).openStream().readAllBytes());

      var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
      assertThat(obdsv2).isNotNull();

      assertThat(mapper.writeMappedXmlString(obdsv2)).isEqualTo(obdsV3String);
    }

    @ParameterizedTest
    @CsvSource({
      "testdaten/obdsv2_keine-tumorzuordung.xml,ADT_GEKID tumorzuordung should not be null at this point - required for oBDS v3",
      "testdaten/obdsv2_nicht-mappbarer-patient.xml,ADT_GEKID tumorzuordung should not be null at this point - required for oBDS v3",
      "testdaten/obdsv2_diagnose-zu-tumorzuordung-keinetumorid.xml,ADT_GEKID attribute 'Tumor_ID' must not be null at this point - required for oBDS v3",
    })
    void shouldNotMapObdsFileAndThrowUnmappableItemException(String obdsV2File, String message)
        throws Exception {
      var obdsV2String =
          new String(
              getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());

      var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
      assertThat(obdsv2).isNotNull();

      var exception =
          assertThrows(UnmappableItemException.class, () -> mapper.writeMappedXmlString(obdsv2));
      assertThat(exception).hasMessage(message);
    }
  }

  @Nested
  class IgnoreUnmappableObdsMapperTest {

    private ObdsMapper mapper;

    @BeforeEach
    void setUp() {
      mapper = ObdsMapper.builder().ignoreUnmappable(true).build();
    }

    @ParameterizedTest
    @CsvSource({
      "testdaten/obdsv2_keine-tumorzuordung.xml,testdaten/obdsv3_keine-tumorzuordung.xml",
      "testdaten/obdsv2_nicht-mappbarer-patient.xml,testdaten/obdsv3_nicht-mappbarer-patient.xml"
    })
    void shouldMapObdsFileIgnoringUnmappableItems(String obdsV2File, String obdsV3File)
        throws Exception {
      var obdsV2String =
          new String(
              getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());
      var obdsV3String =
          new String(
              getClass().getClassLoader().getResource(obdsV3File).openStream().readAllBytes());

      var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
      assertThat(obdsv2).isNotNull();

      assertThat(mapper.writeMappedXmlString(obdsv2)).isEqualTo(obdsV3String);
    }
  }

  @Nested
  class FixMissingIdObdsMapperTest {
    private ObdsMapper mapper;

    @BeforeEach
    void setUp() {
      mapper = ObdsMapper.builder().fixMissingId(true).build();
    }

    @ParameterizedTest
    @CsvSource({
      "testdaten/obdsv2_diagnose-zu-tumorzuordung-keinetumorid.xml,testdaten/obdsv3_diagnose-zu-tumorzuordung-keinetumorid.xml",
      "testdaten/obdsv2_patho.xml,testdaten/obdsv3_patho.xml"
    })
    void shouldMapObdsFileWithGeneratedIds(String obdsV2File, String obdsV3File) throws Exception {
      var obdsV2String =
          new String(
              getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());
      var obdsV3String =
          new String(
              getClass().getClassLoader().getResource(obdsV3File).openStream().readAllBytes());

      var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
      assertThat(obdsv2).isNotNull();

      assertThat(mapper.writeMappedXmlString(obdsv2)).isEqualTo(obdsV3String);
    }
  }

  @Nested
  class DisabledSchemaValidationObdsMapperTest {
    private ObdsMapper mapper;

    @BeforeEach
    void setUp() {
      mapper = ObdsMapper.builder().disableSchemaValidation().build();
    }

    @ParameterizedTest
    @CsvSource({
      "testdaten/obdsv2_invalid-schema.xml,testdaten/obdsv3_invalid-schema.xml",
      "testdaten/obdsv2_invalid-kv.xml,testdaten/obdsv3_invalid-kv.xml",
      "testdaten/obdsv2_st_strahlenart.xml,testdaten/obdsv3_st_strahlenart.xml",
    })
    void shouldMapObdsFileWithoutSchemaValidation(String obdsV2File, String obdsV3File)
        throws Exception {
      var obdsV2String =
          new String(
              getClass().getClassLoader().getResource(obdsV2File).openStream().readAllBytes());
      var obdsV3String =
          new String(
              getClass().getClassLoader().getResource(obdsV3File).openStream().readAllBytes());

      var obdsv2 = mapper.readValue(obdsV2String, ADTGEKID.class);
      assertThat(obdsv2).isNotNull();

      assertThat(mapper.writeMappedXmlString(obdsv2)).isEqualTo(obdsV3String);
    }
  }
}
