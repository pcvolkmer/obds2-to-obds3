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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import de.basisdatensatz.obds.v2.ADTGEKID;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

class ObdsMapperTest {

    @Test
    void shouldMapObdsFile() throws IOException {
        var obdsV2Resource = getClass().getClassLoader().getResource("testdaten/obdsv2_1.xml");
        var obdsV3Resource = getClass().getClassLoader().getResource("testdaten/obdsv3_1.xml");

        final var mapper =
                XmlMapper.builder()
                        .defaultUseWrapper(false)
                        .addModule(new JakartaXmlBindAnnotationModule())
                        .addModule(new Jdk8Module())
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .build();

        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        var obdsv2 = mapper.readValue(obdsV2Resource, ADTGEKID.class);

        assertThat(obdsv2).isNotNull();

        var actual = ObdsMapper.map(obdsv2);
        assertThat(actual).isNotNull();

        assertThat(mapper.writeValueAsString(actual)).isEqualTo(new String(obdsV3Resource.openStream().readAllBytes()));
    }

}
