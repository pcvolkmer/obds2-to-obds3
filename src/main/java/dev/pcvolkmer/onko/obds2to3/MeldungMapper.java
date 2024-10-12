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
import de.basisdatensatz.obds.v3.*;
import de.basisdatensatz.obds.v3.DiagnoseTyp.MengeFruehereTumorerkrankung.FruehereTumorerkrankung;
import de.basisdatensatz.obds.v3.OBDS.MengePatient.Patient.MengeMeldung.Meldung;

import java.util.Optional;

class MeldungMapper {

    public static Meldung map(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException("Source cannot be null");
        }

        var mappedMeldung = new Meldung();
        mappedMeldung.setMeldungID(source.getMeldungID());
        mappedMeldung.setMelderID(source.getMelderID());
        mappedMeldung.setAnmerkung(source.getAnmerkung());

        mappedMeldung.setMeldebegruendung(MeldebegruendungTyp.fromValue(source.getMeldebegruendung()));

        // Nicht in oBDS v2: Eigene Leistung J/N
        // For now: Wenn Melder-ID keine "9999" enthält => "J" -> Onkostar-Konvention für "Extern"
        if (source.getMelderID().contains("9999")) {
            mappedMeldung.setEigeneLeistung(JNUTyp.N);
        } else {
            mappedMeldung.setEigeneLeistung(JNUTyp.J);
        }

        var tumorzuordnung = source.getTumorzuordnung();
        if (tumorzuordnung != null) {
            var mappedTumorzuordnung = new TumorzuordnungTyp();
            mappedTumorzuordnung.setTumorID(tumorzuordnung.getTumorID());
            // Datum
            MapperUtils.mapDateString(tumorzuordnung.getDiagnosedatum()).ifPresent(mappedTumorzuordnung::setDiagnosedatum);
            // ICD10
            var icd10 = new TumorICDTyp();
            icd10.setCode(tumorzuordnung.getPrimaertumorICDCode());
            icd10.setVersion(tumorzuordnung.getPrimaertumorICDVersion());
            mappedTumorzuordnung.setPrimaertumorICD(icd10);
            // Morphologie nicht in oBDS v2 ?
            // Seitenlokalisation: mapping über Enum - beide {'L'|'R'|'B'|'M'|'U'|'T'}
            mappedTumorzuordnung.setSeitenlokalisation(SeitenlokalisationTyp.fromValue(tumorzuordnung.getSeitenlokalisation().value()));

            mappedMeldung.setTumorzuordnung(mappedTumorzuordnung);
        }

        var diagnose = source.getDiagnose();
        if (diagnose != null) {
            var mappedDiagnose = new DiagnoseTyp();
            mappedDiagnose.setPrimaertumorDiagnosetext(diagnose.getPrimaertumorDiagnosetext());
            //mappedDiagnose.setPrimaertumorTopographieICDO(..);
            // Nicht in oBDS v2 ?
            //mappedDiagnose.setPrimaertumorTopographieFreitext(..);
            // oBDS v3 kennt auch 7.1, 7.2 ... als Untertyp von 7 für Diagnosesicherung
            mappedDiagnose.setDiagnosesicherung(diagnose.getDiagnosesicherung());

            if (diagnose.getMengeFruehereTumorerkrankung() != null) {
                mappedDiagnose.getMengeFruehereTumorerkrankung().getFruehereTumorerkrankung().addAll(
                        diagnose.getMengeFruehereTumorerkrankung().getFruehereTumorerkrankung().stream()
                                .map(MeldungMapper::mapFruehereTumorerkrankung)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .toList()
                );
            }

            // Menge Histologie lässt sich nicht direkt auf einen Wert mappen in oBDS v3
            //mappedDiagnose.setHistologie(..);

            // Fernmetastasen
            if (diagnose.getMengeFM() != null) {
                mappedDiagnose.getMengeFM().getFernmetastase().addAll(
                        diagnose.getMengeFM().getFernmetastase().stream()
                                .map(MeldungMapper::mapFernmetastase)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .toList()
                );
            }

            // cTNPM
            mapTnmType(diagnose.getCTNM()).ifPresent(mappedDiagnose::setCTNM);

            // pTNPM
            mapTnmType(diagnose.getPTNM()).ifPresent(mappedDiagnose::setPTNM);

            // Weitere Klassifikationen
            if (diagnose.getMengeWeitereKlassifikation() != null) {
                mappedDiagnose.getMengeWeitereKlassifikation().getWeitereKlassifikation().addAll(
                        diagnose.getMengeWeitereKlassifikation().getWeitereKlassifikation().stream()
                                .map(MeldungMapper::mapWeitereKlassifikation)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .toList()
                );
            }

            // Nicht in oBDS v2 ?
            // mappedDiagnose.setMengeGenetik(..);

            // Direkt übernommen
            // Beide nutzen {'0'|'1'|'2'|'3'|'4'|'U'|'10%'|'20%'|'30%'|'40%'|'50%'|'60%'|'70%'|'80%'|'90%'|'100%'}
            mappedDiagnose.setAllgemeinerLeistungszustand(diagnose.getAllgemeinerLeistungszustand());

            // TODO weitere Diagnose-Module: Allgemein, Mamma, Darm, Prostata, Malignes Melanom
            // Modul DKKR nicht in oDBS v2 ?

            mappedMeldung.setDiagnose(mappedDiagnose);
        }

        // Nicht direkt Mappbar: Meldeanlass -> Untertypen
        // TODO other items: Pathologie, OP, ST, SYST, Verlauf, Tod, Tumorkonferenz, Menge_Zusatzitem

        return mappedMeldung;
    }

    private static Optional<MengeWeitereKlassifikationTyp.WeitereKlassifikation> mapWeitereKlassifikation(de.basisdatensatz.obds.v2.MengeWeitereKlassifikationTyp.WeitereKlassifikation source) {
        if (source != null) {
            var result = new MengeWeitereKlassifikationTyp.WeitereKlassifikation();
            // Datum
            var date = MapperUtils.mapDateStringGenau(source.getDatum());
            if (date.isEmpty()) {
                return Optional.empty();
            } else {
                result.setDatum(date.get());
            }
            return Optional.of(result);
        }

        return Optional.empty();
    }

    private static Optional<MengeFMTyp.Fernmetastase> mapFernmetastase(de.basisdatensatz.obds.v2.MengeFMTyp.Fernmetastase source) {
        if (source != null) {
            var result = new MengeFMTyp.Fernmetastase();
            // Diagnosedatum
            var date = MapperUtils.mapDateString(source.getFMDiagnosedatum());
            if (date.isEmpty()) {
                return Optional.empty();
            } else {
                result.setDiagnosedatum(date.get());
            }
            // Lokalisation - direkt übernommen
            // oBDS v2 kennt 'PUL'|'OSS'|'HEP'|'BRA'|'LYM'|'MAR'|'PLE'|'PER'|'ADR'|'SKI'|'OTH'|'GEN'
            // oBDS v3 kennt 'PUL'|'OSS'|'HEP'|'BRA'|'LYM'|'MAR'|'PLE'|'PER'|'ADR'|'SKI'|'OTH'|'GEN'
            result.setLokalisation(source.getFMLokalisation());
            return Optional.of(result);
        }

        return Optional.empty();
    }

    private static Optional<FruehereTumorerkrankung> mapFruehereTumorerkrankung(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.Diagnose.MengeFruehereTumorerkrankung.FruehereTumorerkrankung source) {
        if (source != null) {
            var result = new FruehereTumorerkrankung();
            // Diagnosedatum
            var date = MapperUtils.mapDateString(source.getDiagnosedatum());
            if (date.isEmpty()) {
                return Optional.empty();
            } else {
                result.setDiagnosedatum(date.get().getValue());
            }
            // ICD10
            var icd10 = new TumorICDTyp();
            icd10.setCode(source.getICDCode());
            icd10.setVersion(source.getICDVersion());
            result.setICD(icd10);
            // Freitext
            result.setFreitext(source.getFreitext());
            return Optional.of(result);
        }

        return Optional.empty();
    }

    private static Optional<TNMTyp> mapTnmType(de.basisdatensatz.obds.v2.TNMTyp source) {
        if (source != null) {
            var result = new TNMTyp();
            var date = MapperUtils.mapDateString(source.getTNMDatum());
            if (date.isEmpty()) {
                return Optional.empty();
            } else {
                result.setDatum(date.get().getValue());
            }
            result.setVersion(source.getTNMVersion());
            result.setYSymbol(source.getTNMYSymbol());
            result.setRSymbol(source.getTNMRSymbol());
            result.setASymbol(source.getTNMASymbol());
            result.setCPUPraefixT(source.getTNMCPUPraefixT());
            result.setT(source.getTNMT());
            result.setMSymbol(source.getTNMMSymbol());
            result.setCPUPraefixN(source.getTNMCPUPraefixN());
            result.setN(source.getTNMN());
            result.setCPUPraefixM(source.getTNMCPUPraefixM());
            result.setM(source.getTNMM());
            result.setL(source.getTNML());
            result.setV(source.getTNMV());
            result.setPn(source.getTNMPn());
            result.setS(source.getTNMS());
            // Nicht in oBDSv2?
            //result.setUICCStadium();
            return Optional.of(result);
        }
        return Optional.empty();
    }

}
