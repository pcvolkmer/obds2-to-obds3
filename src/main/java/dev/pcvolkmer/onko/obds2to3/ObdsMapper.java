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
import de.basisdatensatz.obds.v3.OBDS;

import java.util.Optional;

public class ObdsMapper {

    public static OBDS map(ADTGEKID adtgekid) {
        assert adtgekid != null;

        OBDS obds = new OBDS();
        obds.setSchemaVersion("3.0.3");

        // In oBDSv2 nicht so definiert
        // Letztes Meldedatum aus oBDSv2 Menge_Meldung?
        adtgekid.getMengePatient().getPatient().stream()
                .map(patient -> patient.getMengeMeldung().getMeldung().stream()
                        .map(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung::getMeldedatum)
                        .map(MapperUtils::mapDateString)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .ifPresent(datumTagOderMonatOderJahrOderNichtGenauTyp -> obds.setMeldedatum(datumTagOderMonatOderJahrOderNichtGenauTyp.getValue()));

        // Absender der Meldung
        var absender = adtgekid.getAbsender();
        assert absender != null;
        obds.setAbsender(AbsenderMapper.map(absender));

        // Menge Patient
        var mengePatient = adtgekid.getMengePatient();
        assert mengePatient != null;

        var mappedMengePatient = new OBDS.MengePatient();
        mappedMengePatient.getPatient().addAll(mengePatient.getPatient().stream().map(PatientMapper::map).toList());
        obds.setMengePatient(mappedMengePatient);

        // Menge Melder
        var mengeMelder = adtgekid.getMengeMelder();
        assert mengeMelder != null;

        var mappedMengeMelder = new OBDS.MengeMelder();
        mappedMengeMelder.getMelder().addAll(
                mengeMelder.getMelder().stream().map(MelderMapper::map).toList()
        );
        obds.setMengeMelder(mappedMengeMelder);

        return obds;
    }
}
