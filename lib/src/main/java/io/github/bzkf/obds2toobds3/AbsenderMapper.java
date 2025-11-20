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

import de.basisdatensatz.obds.v2.ADTGEKID;
import de.basisdatensatz.obds.v3.AbsenderTyp;

class AbsenderMapper {

  private AbsenderMapper() {}

  public static AbsenderTyp map(ADTGEKID.Absender source) {
    if (null == source) {
      throw new IllegalArgumentException("Source cannot be null");
    }

    var absender = new AbsenderTyp();
    absender.setAbsenderID(source.getAbsenderID());
    absender.setAnschrift(source.getAbsenderAnschrift());
    absender.setAnsprechpartner(source.getAbsenderAnsprechpartner());
    absender.setBezeichnung(source.getAbsenderBezeichnung());
    absender.setEMail(source.getAbsenderEMail());
    absender.setTelefon(source.getAbsenderTelefon());

    // Fixed Values
    absender.setSoftwareID("obds2-to-obds3");
    absender.setSoftwareVersion("0.1.9"); // x-release-please-version

    return absender;
  }
}
