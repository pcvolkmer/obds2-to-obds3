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

import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;

class MeldungMapper {

    private static final String MUST_NOT_BE_NULL = "ADT_GEKID must not be null at this point";
    private static final String DIAGNOSE_SHOULD_NOT_BE_NULL = "ADT_GEKID diagnose should not be null at this point";
    private static final String TUMORZUORDUNG_SHOULD_NOT_BE_NULL = "ADT_GEKID tumorzuordung should not be null at this point - required for oBDS v3";
    private static final String TUMORID_MUST_NOT_BE_NULL = "ADT_GEKID attribute 'Tumor_ID' must not be null at this point - required for oBDS v3";

    private final boolean ignoreUnmappableMessages;
    private final boolean fixMissingId;

    MeldungMapper(boolean ignoreUnmappableMessages, boolean fixMissingId) {
        this.ignoreUnmappableMessages = ignoreUnmappableMessages;
        this.fixMissingId = fixMissingId;
    }

    /**
     * Mappe eine ADT_GEKID oBDS v2 Meldung in eine Liste von oBDS v3 Meldungen
     *
     * @param source The source message
     * @return
     */
    public List<Meldung> map(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException(MUST_NOT_BE_NULL);
        }

        if (null == source.getTumorzuordnung()
                && (null == source.getDiagnose() || null == source.getDiagnose().getPrimaertumorICDCode()
                        || null == source.getDiagnose().getPrimaertumorICDVersion())) {
            if (ignoreUnmappableMessages) {
                return new ArrayList<>();
            }
            throw new UnmappableItemException(TUMORZUORDUNG_SHOULD_NOT_BE_NULL);
        }

        var result = new ArrayList<Meldung>();
        try {
            // Diagnose als einzelne Meldung
            var diagnosemeldung = getMeldungDiagnose(source);
            // Füge Zusatzitems zur Diagnosemeldung hinzu, wenn vorhanden
            getMengeZusatzitemTyp(source).ifPresent(diagnosemeldung::setMengeZusatzitem);
            result.add(diagnosemeldung);
        } catch (UnmappableItemException e) {
            if (!ignoreUnmappableMessages) {
                throw e;
            }
        }
        // Tumorkonferenzen als einzelne Meldung
        result.addAll(getMappedTumorkonferenzen(source));
        // Verlauf - Ohne: oOBDS v2 Verlauf - Tod
        result.addAll(getMappedVerlauf(source));
        // Verlauf - Hier: oBDS v2 Verlauf - Tod
        getMeldungTod(source).ifPresent(result::add);

        // Für jede Meldung: Ergänze Tumorzuordnung aus Diagnose, wenn möglich und noch
        // nicht vorhanden
        result.forEach(meldung -> {
            if (null == meldung.getTumorzuordnung()) {
                try {
                    getMappedTumorzuordung(source).ifPresent(
                            meldung::setTumorzuordnung);
                } catch (UnmappableItemException e) {
                    if (!ignoreUnmappableMessages) {
                        throw e;
                    }
                }
            }
        });

        // Nicht direkt Mappbar: Meldeanlass -> Untertypen
        // TODO other items: Pathologie, OP, ST, SYST, Menge_Zusatzitem

        return result.stream()
                .filter(meldung -> !ignoreUnmappableMessages
                        || (null != meldung.getTumorzuordnung() && null != meldung.getTumorzuordnung().getTumorID()))
                .toList();
    }

    private Optional<TumorzuordnungTyp> getMappedTumorzuordung(
            ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung meldung) {
        if (null == meldung) {
            return Optional.empty();
        }

        var source = meldung.getDiagnose();

        if (null == source || null == source.getDiagnosedatum() || null == source.getPrimaertumorICDCode()
                || null == source.getPrimaertumorICDVersion()) {
            return Optional.empty();
        }

        var mappedTumorzuordnung = new TumorzuordnungTyp();
        // Übernehme Tumor-ID, wenn vorhanden - ansonsten generiere ID, wenn verlangt
        if (this.fixMissingId
                && null == source.getTumorID()
                && null != source.getDiagnosedatum()
                && null != source.getPrimaertumorICDCode()
                && null != meldung.getMeldungID()) {
            var generatedTumorId = DigestUtils.sha1Hex(
                    String.format("%s_%s_%s", source.getDiagnosedatum(), source.getPrimaertumorICDCode(),
                            meldung.getMeldungID()))
                    .subSequence(0, 16);
            mappedTumorzuordnung.setTumorID(String.format("TID_%s", generatedTumorId));
        } else if (null != source.getTumorID()) {
            mappedTumorzuordnung.setTumorID(source.getTumorID());
        } else {
            throw new UnmappableItemException(TUMORID_MUST_NOT_BE_NULL);
        }
        // Datum
        MapperUtils.mapDateString(source.getDiagnosedatum()).ifPresent(mappedTumorzuordnung::setDiagnosedatum);
        // ICD10
        var icd10 = new TumorICDTyp();
        icd10.setCode(source.getPrimaertumorICDCode());
        icd10.setVersion(source.getPrimaertumorICDVersion());
        mappedTumorzuordnung.setPrimaertumorICD(icd10);
        // Morphologie nicht in oBDS v2 ?
        // Seitenlokalisation: mapping über Enum - beide {'L'|'R'|'B'|'M'|'U'|'T'}
        if (null != source.getSeitenlokalisation()) {
            mappedTumorzuordnung
                    .setSeitenlokalisation(SeitenlokalisationTyp.fromValue(source.getSeitenlokalisation().value()));
        }

        return Optional.of(mappedTumorzuordnung);
    }

    /**
     * Liefert ein Optional mit MengeZusatzitems, wenn im oBDS v2 enthalten und
     * nicht leer
     *
     * @param source
     * @return
     */
    private static Optional<MengeZusatzitemTyp> getMengeZusatzitemTyp(
            ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException(MUST_NOT_BE_NULL);
        }

        var mengeZusatzitem = source.getMengeZusatzitem();
        if (null == mengeZusatzitem) {
            return Optional.empty();
        }

        var mappedZusatzitems = mengeZusatzitem.getZusatzitem().stream()
                .map(zusatzitem -> {
                    var mappedZusatzitem = new MengeZusatzitemTyp.Zusatzitem();
                    mappedZusatzitem.setArt(null != zusatzitem.getArt() ? zusatzitem.getArt().trim() : null);
                    // Nur, wenn in oBDSv2 vorhanden und mappbar
                    MapperUtils.mapDateString(zusatzitem.getDatum()).ifPresent(mappedZusatzitem::setDatum);
                    mappedZusatzitem.setBemerkung(null != zusatzitem.getBemerkung() ? zusatzitem.getBemerkung().trim() : null);
                    mappedZusatzitem.setWert(null != zusatzitem.getWert() ? zusatzitem.getWert().trim() : null);
                    return mappedZusatzitem;
                }).toList();

        if (mappedZusatzitems.isEmpty()) {
            return Optional.empty();
        }

        var result = new MengeZusatzitemTyp();
        result.getZusatzitem().addAll(mappedZusatzitems);
        return Optional.of(result);
    }

    /**
     * Liefert eine Meldung mit Item "tod", wenn im oBDS v2-Verlauf eine Meldung
     * "tod" enthalten ist
     * Die ID der Meldung hat den Suffix "__D"
     *
     * @param source
     * @return
     */
    private Optional<Meldung> getMeldungTod(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException(MUST_NOT_BE_NULL);
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
                    // tod.setAbschlussID();
                    tod.setTodTumorbedingt(JNUTyp.fromValue(verlaufTod.getTodTumorbedingt().value()));
                    // Sterbedatum
                    MapperUtils.mapDateString(verlaufTod.getSterbedatum())
                            .ifPresent(datum -> tod.setSterbedatum(datum.getValue()));
                    // Nicht in oBDS v2?
                    // tod.setModulDKKR(..);
                    if (null != verlaufTod.getMengeTodesursache()
                            && null != verlaufTod.getMengeTodesursache().getTodesursacheICD()) {
                        var mengeTodesursachen = new TodTyp.MengeTodesursachen();
                        mengeTodesursachen.getTodesursacheICD().addAll(
                                verlaufTod.getMengeTodesursache().getTodesursacheICD().stream().map(icd10 -> {
                                    var result = new AllgemeinICDTyp();
                                    result.setCode(icd10);
                                    result.setVersion(verlaufTod.getMengeTodesursache().getTodesursacheICDVersion());
                                    return result;
                                }).toList());
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

    private List<Meldung> getMappedTumorkonferenzen(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException(MUST_NOT_BE_NULL);
        }

        var mengeTumorkonferenz = source.getMengeTumorkonferenz();
        if (null == mengeTumorkonferenz || mengeTumorkonferenz.getTumorkonferenz().isEmpty()) {
            return List.of();
        }

        return mengeTumorkonferenz.getTumorkonferenz().stream()
                .map(tumorkonferenz -> {
                    var mappedTumorkonferenz = new TumorkonferenzTyp();
                    MapperUtils.mapDateString(tumorkonferenz.getTumorkonferenzDatum()).ifPresent(datum -> {
                        mappedTumorkonferenz.setDatum(datum);
                        // oBDS v2 Meldung-Meldeanlass ist Quelle!
                        mappedTumorkonferenz.setMeldeanlass(source.getMeldeanlass());
                        mappedTumorkonferenz.setTumorkonferenzID(tumorkonferenz.getTumorkonferenzID());
                        mappedTumorkonferenz.setTyp(tumorkonferenz.getTumorkonferenzTyp());
                        // Therapieempfehlung nicht in oBDS v2?
                        // mappedTumorkonferenz.setTherapieempfehlung();
                    });
                    return new AbstractMap.SimpleEntry<String, TumorkonferenzTyp>(tumorkonferenz.getAnmerkung(),
                            mappedTumorkonferenz);
                })
                .map(anmerkungAndMappedTumorkonferenz -> {
                    var anmerkung = anmerkungAndMappedTumorkonferenz.getKey();
                    var mappedTumorkonferenz = anmerkungAndMappedTumorkonferenz.getValue();
                    var meldung = getMeldungsRumpf(source);
                    meldung.setMeldungID(
                            String.format("%s_%s", source.getMeldungID(), mappedTumorkonferenz.getTumorkonferenzID()));
                    meldung.setTumorkonferenz(mappedTumorkonferenz);
                    // Anmerkung übernommen aus oBDS v2 -> Meldung-Tumorkonferenz
                    meldung.setAnmerkung(anmerkung);
                    return meldung;
                })
                .toList();
    }

    private List<Meldung> getMappedVerlauf(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException(MUST_NOT_BE_NULL);
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
                    MapperUtils.mapDateString(verlauf.getUntersuchungsdatumVerlauf())
                            .ifPresent(datum -> mappedVerlauf.setUntersuchungsdatumVerlauf(datum.getValue()));
                    mappedVerlauf.setGesamtbeurteilungTumorstatus(verlauf.getGesamtbeurteilungTumorstatus());

                    // Fernmetastasen
                    if (verlauf.getMengeFM() != null) {
                        mappedVerlauf.getMengeFM().getFernmetastase().addAll(
                                verlauf.getMengeFM().getFernmetastase().stream()
                                        .map(MeldungMapper::mapFernmetastase)
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .toList());
                    }

                    // TNPM
                    mapTnmType(verlauf.getTNM()).ifPresent(mappedVerlauf::setTNM);

                    // Nicht in oBDS v2 enthalten ?
                    // mappedVerlauf.setMengeGenetik(..);

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

    private Meldung getMeldungDiagnose(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        var diagnose = source.getDiagnose();
        if (null == diagnose) {
            throw new IllegalArgumentException(DIAGNOSE_SHOULD_NOT_BE_NULL);
        }

        var mappedDiagnose = new DiagnoseTyp();
        mappedDiagnose.setPrimaertumorDiagnosetext(diagnose.getPrimaertumorDiagnosetext());
        // mappedDiagnose.setPrimaertumorTopographieICDO(..);
        // Nicht in oBDS v2 ?
        // mappedDiagnose.setPrimaertumorTopographieFreitext(..);
        // oBDS v3 kennt auch 7.1, 7.2 ... als Untertyp von 7 für Diagnosesicherung
        mappedDiagnose.setDiagnosesicherung(diagnose.getDiagnosesicherung());

        if (diagnose.getMengeFruehereTumorerkrankung() != null) {
            mappedDiagnose.getMengeFruehereTumorerkrankung().getFruehereTumorerkrankung().addAll(
                    diagnose.getMengeFruehereTumorerkrankung().getFruehereTumorerkrankung().stream()
                            .map(MeldungMapper::mapFruehereTumorerkrankung)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList());
        }

        // Menge Histologie lässt sich nicht direkt auf einen Wert mappen in oBDS v3 -
        // mehrere Diagnose-Meldungen?
        // mappedDiagnose.setHistologie(..);
        // Aktuell: Immer nur erste Histologie verwendet!
        if (diagnose.getMengeHistologie() != null) {
            diagnose.getMengeHistologie().getHistologie().stream().findFirst().map(firstHisto -> {
                var mappedHisto = new HistologieTyp();
                mappedHisto.setGrading(firstHisto.getGrading());
                mappedHisto.setHistologieID(firstHisto.getHistologieID());
                mappedHisto.setHistologieEinsendeNr(firstHisto.getHistologieEinsendeNr());
                mappedHisto.setLKBefallen(firstHisto.getLKBefallen());
                mappedHisto.setLKUntersucht(firstHisto.getLKUntersucht());
                mappedHisto.setMorphologieFreitext(firstHisto.getMorphologieFreitext());
                mappedHisto.setSentinelLKBefallen(firstHisto.getSentinelLKBefallen());
                mappedHisto.setSentinelLKUntersucht(firstHisto.getSentinelLKUntersucht());
                // Nur wenn vorhanden und mappbar
                MapperUtils.mapDateString(firstHisto.getTumorHistologiedatum())
                        .ifPresent(mappedHisto::setTumorHistologiedatum);
                return mappedHisto;
            })
                    // Wenn Histo vorhanden - erste Histo
                    .ifPresent(mappedDiagnose::setHistologie);
        }

        // Fernmetastasen
        if (diagnose.getMengeFM() != null) {
            mappedDiagnose.getMengeFM().getFernmetastase().addAll(
                    diagnose.getMengeFM().getFernmetastase().stream()
                            .map(MeldungMapper::mapFernmetastase)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList());
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
                            .toList());
        }

        // Modul Prostata
        if (diagnose.getModulProstata() != null) {
            mappedDiagnose.setModulProstata(ModulMapper.map(diagnose.getModulProstata()));
        }

        // Nicht in oBDS v2 ?
        // mappedDiagnose.setMengeGenetik(..);

        // Direkt übernommen - sofern vorhanden, sonst 'U'
        // Beide nutzen
        // {'0'|'1'|'2'|'3'|'4'|'U'|'10%'|'20%'|'30%'|'40%'|'50%'|'60%'|'70%'|'80%'|'90%'|'100%'}
        if (diagnose.getAllgemeinerLeistungszustand() != null) {
            mappedDiagnose.setAllgemeinerLeistungszustand(diagnose.getAllgemeinerLeistungszustand());
        } else {
            mappedDiagnose.setAllgemeinerLeistungszustand("U");
        }

        var mappedMeldung = getMeldungsRumpf(source);
        mappedMeldung.setMeldungID(source.getMeldungID());
        mappedMeldung.setDiagnose(mappedDiagnose);
        return mappedMeldung;
    }

    private Meldung getMeldungsRumpf(ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung source) {
        if (null == source) {
            throw new IllegalArgumentException("Source cannot be null");
        }

        var mappedMeldung = new Meldung();
        mappedMeldung.setMelderID(source.getMelderID());
        mappedMeldung.setAnmerkung(source.getAnmerkung());

        mappedMeldung.setMeldebegruendung(MeldebegruendungTyp.fromValue(source.getMeldebegruendung()));

        // Nicht in oBDS v2: Eigene Leistung J/N
        // For now: Wenn Melder-ID keine "9999" enthält => "J" -> Onkostar-Konvention
        // für "Extern"
        if (source.getMelderID().contains("9999")) {
            mappedMeldung.setEigeneLeistung(JNUTyp.N);
        } else {
            mappedMeldung.setEigeneLeistung(JNUTyp.J);
        }

        // In jeder (Unter-)Meldung
        var tumorzuordnung = source.getTumorzuordnung();
        if (tumorzuordnung != null) {
            var mappedTumorzuordnung = new TumorzuordnungTyp();
            // Übernehme Tumor-ID, wenn vorhanden
            if (fixMissingId
                    && null == tumorzuordnung.getTumorID()
                    && null != tumorzuordnung.getDiagnosedatum()
                    && null != tumorzuordnung.getPrimaertumorICDCode()
                    && null != source.getMeldungID()) {
                var generatedTumorId = DigestUtils.sha1Hex(
                        String.format("%s_%s_%s", tumorzuordnung.getDiagnosedatum(),
                                tumorzuordnung.getPrimaertumorICDCode(), source.getMeldungID()))
                        .subSequence(0, 16);
                mappedTumorzuordnung.setTumorID(String.format("TID_%s", generatedTumorId));
            } else if (null != tumorzuordnung.getTumorID()) {
                mappedTumorzuordnung.setTumorID(tumorzuordnung.getTumorID());
            } else {
                throw new UnmappableItemException(TUMORID_MUST_NOT_BE_NULL);
            }
            // Datum
            MapperUtils.mapDateString(tumorzuordnung.getDiagnosedatum())
                    .ifPresent(mappedTumorzuordnung::setDiagnosedatum);
            // ICD10
            var icd10 = new TumorICDTyp();
            icd10.setCode(tumorzuordnung.getPrimaertumorICDCode());
            icd10.setVersion(tumorzuordnung.getPrimaertumorICDVersion());
            mappedTumorzuordnung.setPrimaertumorICD(icd10);
            // Morphologie nicht in oBDS v2 ?
            // Seitenlokalisation: mapping über Enum - beide {'L'|'R'|'B'|'M'|'U'|'T'}
            if (null != tumorzuordnung.getSeitenlokalisation()) {
                mappedTumorzuordnung.setSeitenlokalisation(
                        SeitenlokalisationTyp.fromValue(tumorzuordnung.getSeitenlokalisation().value()));
            } else {
                mappedTumorzuordnung.setSeitenlokalisation(SeitenlokalisationTyp.U);
            }

            mappedMeldung.setTumorzuordnung(mappedTumorzuordnung);
        }

        return mappedMeldung;
    }

    private static Optional<MengeWeitereKlassifikationTyp.WeitereKlassifikation> mapWeitereKlassifikation(
            de.basisdatensatz.obds.v2.MengeWeitereKlassifikationTyp.WeitereKlassifikation source) {
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

    private static Optional<MengeFMTyp.Fernmetastase> mapFernmetastase(
            de.basisdatensatz.obds.v2.MengeFMTyp.Fernmetastase source) {
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
            // oBDS v2 kennt
            // 'PUL'|'OSS'|'HEP'|'BRA'|'LYM'|'MAR'|'PLE'|'PER'|'ADR'|'SKI'|'OTH'|'GEN'
            // oBDS v3 kennt
            // 'PUL'|'OSS'|'HEP'|'BRA'|'LYM'|'MAR'|'PLE'|'PER'|'ADR'|'SKI'|'OTH'|'GEN'
            result.setLokalisation(source.getFMLokalisation());
            return Optional.of(result);
        }

        return Optional.empty();
    }

    private static Optional<FruehereTumorerkrankung> mapFruehereTumorerkrankung(
            ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.Diagnose.MengeFruehereTumorerkrankung.FruehereTumorerkrankung source) {
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
            // result.setUICCStadium();
            return Optional.of(result);
        }
        return Optional.empty();
    }

}
