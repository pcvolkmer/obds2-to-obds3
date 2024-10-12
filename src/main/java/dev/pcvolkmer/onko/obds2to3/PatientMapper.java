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
import de.basisdatensatz.obds.v3.PatientenAdresseMelderTyp;
import de.basisdatensatz.obds.v3.PatientenStammdatenMelderTyp;
import de.basisdatensatz.obds.v3.VersichertendatenGKVTyp;

import java.util.List;

class PatientMapper {

    private PatientMapper() {}

    public static OBDS.MengePatient.Patient map(ADTGEKID.MengePatient.Patient source) {
        if (null == source) {
            throw new IllegalArgumentException("Source cannot be null");
        }

        var stammdaten = source.getPatientenStammdaten();
        assert stammdaten != null;

        var patient = new OBDS.MengePatient.Patient();
        // PatientenID in oBDS v2 in Stammdaten
        patient.setPatientID(stammdaten.getPatientID());
        patient.setAnmerkung(source.getAnmerkung());
        patient.setPatientenStammdaten(getMappedStammdaten(stammdaten));

        // Meldungen
        var mappedMeldungen = source.getMengeMeldung().getMeldung().stream()
                .map(MeldungMapper::map)
                .toList();

        patient.setMengeMeldung(new OBDS.MengePatient.Patient.MengeMeldung());
        patient.getMengeMeldung().getMeldung().addAll(mappedMeldungen);
        
        return patient;
    }

    private static PatientenStammdatenMelderTyp getMappedStammdaten(ADTGEKID.MengePatient.Patient.PatientenStammdaten stammdaten) {
        // Stammdaten
        var mappedStammdaten = new PatientenStammdatenMelderTyp();

        // Adresse
        // TODO Aktuell gültige Adresse verwenden
        stammdaten.getMengeAdresse().getAdresse().stream().findFirst().ifPresent(adresse -> {
            var mappedAdresse = new PatientenAdresseMelderTyp();
            mappedAdresse.setHausnummer(adresse.getPatientenHausnummer());
            mappedAdresse.setStrasse(adresse.getPatientenStrasse());
            mappedAdresse.setPLZ(adresse.getPatientenPLZ());
            mappedAdresse.setOrt(adresse.getPatientenOrt());
            mappedAdresse.setLand(adresse.getPatientenLand());
            mappedStammdaten.setAdresse(mappedAdresse);
        });

        // Stammdaten - Krankenkasse: In oBDS v2 keine Unterscheidung GKV/PKV
        // Aktuell als GKV behandelt
        var versichertendatenGkv = new VersichertendatenGKVTyp();
        versichertendatenGkv.setIKNR(stammdaten.getKrankenkassenNr());
        versichertendatenGkv.setGKVVersichertennummer(stammdaten.getKrankenversichertenNr());
        mappedStammdaten.setVersichertendatenGKV(versichertendatenGkv);

        // Geburtsdatum
        var geburtsdatum = MapperUtils.mapDateString(stammdaten.getPatientenGeburtsdatum());
        assert geburtsdatum.isPresent();
        mappedStammdaten.setGeburtsdatum(geburtsdatum.get());

        // Geschlecht
        // oBDS v2 kennt noch "S", oBDS v3 jedoch "D" und "X" - daher "U" für unbekannt
        var geschlecht = stammdaten.getPatientenGeschlecht();
        if (geschlecht != null && List.of("M", "W").contains(geschlecht)) {
            mappedStammdaten.setGeschlecht(geschlecht);
        } else {
            mappedStammdaten.setGeschlecht("U");
        }

        // Name(n)
        mappedStammdaten.setGeburtsname(stammdaten.getPatientenGeburtsname());
        // Namensvorsatz nicht in oBDS v2
        mappedStammdaten.setNamenszusatz(stammdaten.getPatientenNamenszusatz());
        mappedStammdaten.setTitel(stammdaten.getPatientenTitel());
        mappedStammdaten.setNachname(stammdaten.getPatientenNachname());
        mappedStammdaten.setVornamen(stammdaten.getPatientenVornamen());
        // Frühere Namen
        var mappedFruehereNamen = new PatientenStammdatenMelderTyp.MengeFruehererName();
        if (stammdaten.getMengeFruehererName() != null) {
            mappedFruehereNamen.getFruehererName().addAll(stammdaten.getMengeFruehererName().getPatientenFruehererName());
        }
        mappedStammdaten.setMengeFruehererName(mappedFruehereNamen);

        return mappedStammdaten;
    }

}
