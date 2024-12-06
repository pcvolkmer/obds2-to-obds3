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

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;

/** Validator for ADT_GEKID or oBDS files
 *
 * @author Paul-Christian Volkmer
 * @since 0.1.0
 */
public class SchemaValidator {

    /** Validates xml string using given schema version
     *
     * @param xmlString The xml string to be validated
     * @param schemaVersion The schema version to be used
     * @return true, if xml string is valid
     * @throws SchemaValidatorException if xmlString is not valid
     */
    public static boolean isValid(String xmlString, SchemaVersion schemaVersion) {
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            var schemaFile = new StreamSource(SchemaValidator.class.getClassLoader().getResource(schemaVersion.getSchemaFile()).openStream());
            var validator = factory.newSchema(schemaFile).newValidator();
            validator.validate(new StreamSource(new StringReader(xmlString)));
        } catch (Exception e) {
            throw new SchemaValidatorException("Cannot validate result using oBDS schema", e);
        }
        return true;
    }

    public enum SchemaVersion {
        ADT_GEKID_2_2_3("schema/ADT_GEKID_v2.2.3.xsd"),
        OBDS_3_0_3("schema/oBDS_v3.0.3.xsd"),
        ;

        private final String schemaFile;

        SchemaVersion(String schemaFile) {
            this.schemaFile = schemaFile;
        }

        String getSchemaFile() {
            return schemaFile;
        }
    }

    public static class SchemaValidatorException extends RuntimeException {
        public SchemaValidatorException(String message) {
            super(message);
        }

        public SchemaValidatorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}