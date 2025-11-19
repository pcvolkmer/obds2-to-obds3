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

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.SAXException;

/**
 * Validator for ADT_GEKID or oBDS files
 *
 * @author Paul-Christian Volkmer
 * @since 0.1.0
 */
public class SchemaValidator {

  /**
   * Validates xml string using given schema version
   *
   * @param xmlString The xml string to be validated
   * @param schemaVersion The schema version to be used
   * @return true, if xml string is valid
   * @throws SchemaValidatorException if xmlString is not valid
   */
  public static boolean isValid(String xmlString, SchemaVersion schemaVersion) {
    try {
      var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // Prevent XXE: Disable DTDs and external entities
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      var schemaFile =
          new StreamSource(
              SchemaValidator.class
                  .getClassLoader()
                  .getResource(schemaVersion.getSchemaFile())
                  .openStream());
      var validator = factory.newSchema(schemaFile).newValidator();
      validator.validate(new StreamSource(new StringReader(xmlString)));
    } catch (Exception e) {
      throw new SchemaValidatorException("Cannot validate result using oBDS schema", e);
    }
    return true;
  }

  /**
   * Returns regexp pattern of string based datatypes defined in given schema
   *
   * @param name The name of the datatype
   * @param schemaVersion The schema version
   * @return returns valid <code>Pattern</code> or empty <code>Optional</code>
   */
  public static Optional<Pattern> regexpPattern(
      String name, SchemaValidator.SchemaVersion schemaVersion) {
    try {
      final var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final var schemaFile =
          new StreamSource(
              SchemaValidator.class
                  .getClassLoader()
                  .getResource(schemaVersion.getSchemaFile())
                  .openStream());
      final var doc = documentBuilder.parse(schemaFile.getInputStream());

      final var expr =
          XPathFactory.newInstance()
              .newXPath()
              .compile(
                  String.format(
                      "//*[local-name()='simpleType'][@name='%s']/*[local-name()='restriction']/*[local-name()='pattern']/@value",
                      name));

      return Optional.of(Pattern.compile(expr.evaluate(doc, XPathConstants.STRING).toString()));
    } catch (XPathExpressionException
        | ParserConfigurationException
        | IOException
        | SAXException e) {
      return Optional.empty();
    }
  }

  public enum SchemaVersion {
    ADT_GEKID_2_2_3("schema/ADT_GEKID_v2.2.3.xsd"),
    OBDS_3_0_3("schema/oBDS_v3.0.3.xsd"),
    OBDS_3_0_4("schema/oBDS_v3.0.4.xsd"),
    ;

    private final String schemaFile;

    SchemaVersion(String schemaFile) {
      this.schemaFile = schemaFile;
    }

    String getSchemaFile() {
      return schemaFile;
    }
  }
}
