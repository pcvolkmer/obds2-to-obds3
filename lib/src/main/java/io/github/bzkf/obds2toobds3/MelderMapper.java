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

import de.basisdatensatz.obds.v3.IdentNummernTyp;
import de.basisdatensatz.obds.v3.MelderTyp;

class MelderMapper {

  private MelderMapper() {}

  public static MelderTyp map(de.basisdatensatz.obds.v2.MelderTyp source) {
    if (null == source) {
      throw new IllegalArgumentException("Source cannot be null");
    }

    var mappedMelder = new MelderTyp();
    mappedMelder.setID(source.getMelderID());
    mappedMelder.setAnschrift(source.getMelderAnschrift());
    mappedMelder.setArztname(source.getMelderArztname());
    mappedMelder.setBankname(source.getMelderBankname());
    mappedMelder.setOrt(source.getMelderOrt());
    mappedMelder.setPLZ(source.getMelderPLZ());
    mappedMelder.setBIC(source.getMelderBIC());
    mappedMelder.setIBAN(source.getMelderIBAN());
    mappedMelder.setKontoinhaber(source.getMelderKontoinhaber());
    mappedMelder.setKHAbtStationPraxis(source.getMelderKHAbtStationPraxis());

    var identNummern = new IdentNummernTyp();
    identNummern.setIKNR(source.getMelderIKNR());
    identNummern.setBSNR(source.getMelderBSNR());
    identNummern.setLANR(source.getMelderLANR());
    // Nicht in oBDS v2 ?
    // identNummern.setZANR();
    mappedMelder.setIdentNummern(identNummern);

    // nicht in oBDS v3 ?
    // source.getMeldendeStelle();
    return mappedMelder;
  }
}
