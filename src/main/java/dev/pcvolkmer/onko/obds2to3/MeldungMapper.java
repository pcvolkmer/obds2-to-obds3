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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class MeldungMapper {

    private MeldungMapper() {}

    /**
     * Mappe eine ADT_GEKID oBDS v2 Meldung in eine Liste von oBDS v3 Meldungen
     *
     * @param source
     * @return
     */
    public static List<Meldung> map(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        var result = new ArrayList<Meldung>();
        result.add(getMeldungDiagnose(source));
        // Ohne: oOBDS v2 Verlauf - Tod
        result.addAll(getMappedVerlauf(source));
        // Hier: oBDS v2 Verlauf - Tod
        getMeldungTod(source).ifPresent(result::add);

        // Nicht direkt Mappbar: Meldeanlass -> Untertypen
        // TODO other items: Pathologie, OP, ST, SYST, Tumorkonferenz, Menge_Zusatzitem

        return result;
    }

    /** Liefert eine Meldung mit Item "tod", wenn im oBDS v2-Verlauf eine Meldung "tod" enthalten ist
     * Die ID der Meldung hat den Suffix "__D"
     *
     * @param source
     * @return
     */
    private static Optional<Meldung> getMeldungTod(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException("ADT_GEKID must not be null at this point");
        }

        var meldung = getMeldungsRumpf(source);
        meldung.setMeldungID(String.format("%s__D", source.getMeldungID()));

        var mengeVerlauf = source.getMengeVerlauf();
        if (null == mengeVerlauf || mengeVerlauf.getVerlauf().isEmpty()) {
            return Optional.empty();
        }

        mengeVerlauf.getVerlauf().stream()
                .map(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeVerlauf.Verlauf::getTod)
                .filter(Objects::nonNull)
                .map(verlaufTod -> {
                    var tod = new TodTyp();
                    // Nicht in oBDS v2?
                    //tod.setAbschlussID();
                    tod.setTodTumorbedingt(JNUTyp.fromValue(verlaufTod.getTodTumorbedingt().value()));
                    // Sterbedatum
                    MapperUtils.mapDateString(verlaufTod.getSterbedatum()).ifPresent(datum -> tod.setSterbedatum(datum.getValue()));
                    // Nicht in oBDS v2?
                    //tod.setModulDKKR(..);
                    if (null != verlaufTod.getMengeTodesursache() && null != verlaufTod.getMengeTodesursache().getTodesursacheICD()) {
                        var mengeTodesursachen = new TodTyp.MengeTodesursachen();
                        mengeTodesursachen.getTodesursacheICD().addAll(
                                verlaufTod.getMengeTodesursache().getTodesursacheICD().stream().map(icd10 -> {
                                    var result = new AllgemeinICDTyp();
                                    result.setCode(icd10);
                                    result.setVersion(verlaufTod.getMengeTodesursache().getTodesursacheICDVersion());
                                    return result;
                                }).toList()
                        );
                        tod.setMengeTodesursachen(mengeTodesursachen);
                    }
                    return tod;
                })
                .findFirst()
                .ifPresent(meldung::setTod);

        if (null != meldung.getTod()) {
            return Optional.of(meldung);
        }
        return Optional.empty();
    }

    private static List<Meldung> getMappedVerlauf(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException("ADT_GEKID must not be null at this point");
        }

        var mengeVerlauf = source.getMengeVerlauf();
        if (null == mengeVerlauf || mengeVerlauf.getVerlauf().isEmpty()) {
            return List.of();
        }

        return mengeVerlauf.getVerlauf().stream()
                // Ohne Verlauf - Tod!
                .filter(verlauf -> verlauf.getTod() == null)
                .map(verlauf -> {
                    var mappedVerlauf = new VerlaufTyp();
                    mappedVerlauf.setVerlaufID(verlauf.getVerlaufID());
                    mappedVerlauf.setMeldeanlass(source.getMeldeanlass());
                    mappedVerlauf.setAllgemeinerLeistungszustand(verlauf.getAllgemeinerLeistungszustand());
                    // oBDS v2 Meldung->Meldeanlass wird in oBDS v3 für Verlauf verwendet
                    mappedVerlauf.setMeldeanlass(source.getMeldeanlass());
                    mappedVerlauf.setVerlaufLokalerTumorstatus(verlauf.getVerlaufLokalerTumorstatus());
                    mappedVerlauf.setVerlaufTumorstatusFernmetastasen(verlauf.getVerlaufTumorstatusFernmetastasen());
                    mappedVerlauf.setVerlaufTumorstatusLymphknoten(verlauf.getVerlaufTumorstatusLymphknoten());

                    // TODO mappedVerlauf.setHistologie();

                    // Nur verwendet, wenn Datumstring mappbar
                    MapperUtils.mapDateString(verlauf.getUntersuchungsdatumVerlauf()).ifPresent(datum -> mappedVerlauf.setUntersuchungsdatumVerlauf(datum.getValue()));
                    mappedVerlauf.setGesamtbeurteilungTumorstatus(verlauf.getGesamtbeurteilungTumorstatus());

                    // Fernmetastasen
                    if (verlauf.getMengeFM() != null) {
                        mappedVerlauf.getMengeFM().getFernmetastase().addAll(
                                verlauf.getMengeFM().getFernmetastase().stream()
                                        .map(MeldungMapper::mapFernmetastase)
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .toList()
                        );
                    }

                    // TNPM
                    mapTnmType(verlauf.getTNM()).ifPresent(mappedVerlauf::setTNM);

                    // Nicht in oBDS v2 enthalten ?
                    //mappedVerlauf.setMengeGenetik(..);

                    // TODO Weitere Module ...
                    return mappedVerlauf;
                })
                .map(mappedVerlauf -> {
                    var meldung = getMeldungsRumpf(source);
                    meldung.setMeldungID(String.format("%s_%s", source.getMeldungID(), mappedVerlauf.getVerlaufID()));
                    meldung.setVerlauf(mappedVerlauf);
                    return meldung;
                })
                .toList();
    }

    private static Meldung getMeldungDiagnose(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        var diagnose = source.getDiagnose();
        if (null == diagnose) {
            throw new IllegalArgumentException("ADT_GEKID diagnose should not be null at this point");
        }

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

        // Menge Histologie lässt sich nicht direkt auf einen Wert mappen in oBDS v3 - mehrere Diagnose-Meldungen?
        // mappedDiagnose.setHistologie(..);

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

        var mappedMeldung = getMeldungsRumpf(source);
        mappedMeldung.setMeldungID(source.getMeldungID());
        mappedMeldung.setDiagnose(mappedDiagnose);
        return mappedMeldung;
    }

    private static Meldung getMeldungsRumpf(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException("Source cannot be null");
        }

        var mappedMeldung = new Meldung();
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

        // In jeder (Unter-)Meldung
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
