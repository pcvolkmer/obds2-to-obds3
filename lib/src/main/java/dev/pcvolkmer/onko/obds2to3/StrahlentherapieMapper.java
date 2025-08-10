package dev.pcvolkmer.onko.obds2to3;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeST.ST.MengeNebenwirkung;
import de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeST.ST.MengeBestrahlung.Bestrahlung;
import de.basisdatensatz.obds.v3.NebenwirkungTyp;
import de.basisdatensatz.obds.v3.NebenwirkungTyp.MengeNebenwirkung.Nebenwirkung;
import de.basisdatensatz.obds.v3.OPTyp;
import de.basisdatensatz.obds.v3.RTyp;
import de.basisdatensatz.obds.v3.STTyp;
import de.basisdatensatz.obds.v3.SeiteZielgebietTyp;
import de.basisdatensatz.obds.v3.StrahlendosisTyp;
import de.basisdatensatz.obds.v3.ZielgebietTyp;
import de.basisdatensatz.obds.v3.OPTyp.Komplikationen;
import de.basisdatensatz.obds.v3.OPTyp.MengeOPS;
import de.basisdatensatz.obds.v3.OPTyp.MengeOPS.OPS;
import de.basisdatensatz.obds.v3.STTyp.Meldeanlass;
import de.basisdatensatz.obds.v3.STTyp.MengeBestrahlung;
import de.basisdatensatz.obds.v3.STTyp.MengeBestrahlung.Bestrahlung.Applikationsart;

public class StrahlentherapieMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StrahlentherapieMapper.class);

    private StrahlentherapieMapper() {
    }

    public static List<STTyp> map(
            de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeST mengeST,
            String meldeanlass) {
        var result = new ArrayList<STTyp>();
        for (var source : mengeST.getST()) {
            var stTyp = new STTyp();
            stTyp.setSTID(source.getSTID());

            stTyp.setMeldeanlass(Meldeanlass.fromValue(meldeanlass));

            // v2: K, P, S, X
            // v3: K, P, S, X, O
            stTyp.setIntention(source.getSTIntention());

            // v2: O, A, N, I, S
            // v3: O, A, N, I, S, Z
            stTyp.setStellungOP(source.getSTStellungOP());

            // v2: A, E, V, P, S, U
            // v3: A, E, V, P, S, U, F, T
            stTyp.setEndeGrund(source.getSTEndeGrund());

            var mengeBestrahlung = new MengeBestrahlung();

            for (var bestrahlungV2 : source.getMengeBestrahlung().getBestrahlung()) {
                var bestrahlungV3 = new STTyp.MengeBestrahlung.Bestrahlung();

                var mappedApplikationsart = mapApplikationsart(bestrahlungV2);
                bestrahlungV3.setApplikationsart(mappedApplikationsart);

                MapperUtils.mapDateString(bestrahlungV2.getSTBeginnDatum())
                        .ifPresent(d -> bestrahlungV3.setBeginn(d.getValue()));
                MapperUtils.mapDateString(bestrahlungV2.getSTEndeDatum())
                        .ifPresent(d -> bestrahlungV3.setEnde(d.getValue()));

                mengeBestrahlung.getBestrahlung().add(bestrahlungV3);
            }

            stTyp.setMengeBestrahlung(mengeBestrahlung);

            if (source.getMengeNebenwirkung() != null) {
                var mappedNebenwirkungen = mapNebenwirkungen(source.getMengeNebenwirkung());
                stTyp.setNebenwirkungen(mappedNebenwirkungen);
            }

            result.add(stTyp);
        }

        return result;
    }

    private static NebenwirkungTyp mapNebenwirkungen(MengeNebenwirkung mengeNebenwirkung) {
        var mengeNebenwirkungV3 = new de.basisdatensatz.obds.v3.NebenwirkungTyp.MengeNebenwirkung();

        var result = new NebenwirkungTyp();

        for (var nebenwirkungV2 : mengeNebenwirkung.getSTNebenwirkung()) {
            var nebenwirkungV3 = new Nebenwirkung();

            if (nebenwirkungV2.getNebenwirkungArt() != null && (nebenwirkungV2.getNebenwirkungArt().equals("K")
                    || nebenwirkungV2.getNebenwirkungArt().equals("U"))) {
                if (mengeNebenwirkung.getSTNebenwirkung().size() > 1) {
                    LOG.warn(
                            "ST has multiple Nebenwirkungen, but should only contain one if set to 'K' or 'U'.");
                }

                result.setGradMaximal2OderUnbekannt(nebenwirkungV2.getNebenwirkungArt());
                return result;
            }

            var art = new NebenwirkungTyp.MengeNebenwirkung.Nebenwirkung.Art();
            if (nebenwirkungV2.getNebenwirkungArt() != null) {
                art.setBezeichnung(nebenwirkungV2.getNebenwirkungArt());
            } else {
                LOG.warn("Nebenwirkung_Art is unset. Defaulting to 'Sonstige'.");
                art.setBezeichnung("Sonstige");
            }

            nebenwirkungV3.setArt(art);

            if (nebenwirkungV2.getNebenwirkungVersion() != null) {
                nebenwirkungV3.setVersion(nebenwirkungV2.getNebenwirkungVersion());
            } else {
                nebenwirkungV3.setVersion("Sonstige");
            }

            nebenwirkungV3.setGrad(nebenwirkungV2.getNebenwirkungGrad());

            mengeNebenwirkungV3.getNebenwirkung().add(nebenwirkungV3);
        }

        result.setMengeNebenwirkung(mengeNebenwirkungV3);
        return result;
    }

    private static Applikationsart mapApplikationsart(Bestrahlung bestrahlung) {
        var result = new Applikationsart();

        var zielgebiet = new ZielgebietTyp();
        zielgebiet.setCodeVersion2014(bestrahlung.getSTZielgebiet());

        var seiteZielgebiet = SeiteZielgebietTyp.fromValue(bestrahlung.getSTSeiteZielgebiet());

        StrahlendosisTyp einzeldosis = null;
        if (bestrahlung.getSTEinzeldosis() != null) {
            einzeldosis = new StrahlendosisTyp();
            einzeldosis.setDosis(bestrahlung.getSTEinzeldosis().getDosis());
            einzeldosis.setEinheit(bestrahlung.getSTEinzeldosis().getEinheit());
        }

        StrahlendosisTyp gesamtdosis = null;
        if (bestrahlung.getSTGesamtdosis() != null) {
            gesamtdosis = new StrahlendosisTyp();
            gesamtdosis.setDosis(bestrahlung.getSTGesamtdosis().getDosis());
            gesamtdosis.setEinheit(bestrahlung.getSTGesamtdosis().getEinheit());
        }

        var applikationsart = bestrahlung.getSTApplikationsart();
        if (applikationsart == null) {
            LOG.warn("Applikationsart is unset. Defaulting to 'Sonstige'");
            applikationsart = "S";
        }

        if (applikationsart.startsWith("P")) {
            var perkutan = new Applikationsart.Perkutan();
            perkutan.setZielgebiet(zielgebiet);
            perkutan.setSeiteZielgebiet(seiteZielgebiet);
            perkutan.setEinzeldosis(einzeldosis);
            perkutan.setGesamtdosis(gesamtdosis);
            switch (applikationsart) {
                case "P":
                    result.setPerkutan(perkutan);
                    break;
                case "PRCJ":
                    perkutan.setRadiochemo("RCJ");
                    result.setPerkutan(perkutan);
                    break;
                case "PRCN":
                    perkutan.setRadiochemo("RCN");
                    result.setPerkutan(perkutan);
                    break;
                default:
                    LOG.warn("Unknown radiation type: {}", applikationsart);
            }

            result.setPerkutan(perkutan);
            return result;
        }

        if (applikationsart.startsWith("K")) {
            var kontakt = new Applikationsart.Kontakt();
            kontakt.setZielgebiet(zielgebiet);
            kontakt.setSeiteZielgebiet(seiteZielgebiet);
            kontakt.setEinzeldosis(einzeldosis);
            kontakt.setGesamtdosis(gesamtdosis);
            switch (applikationsart) {
                case "K":
                    result.setKontakt(kontakt);
                    break;
                case "KHDR":
                    kontakt.setInterstitiellEndokavitaer("K");
                    kontakt.setRateType("HDR");
                    result.setKontakt(kontakt);
                    break;
                case "KPDR":
                    kontakt.setInterstitiellEndokavitaer("K");
                    kontakt.setRateType("PDR");
                    result.setKontakt(kontakt);
                    break;
                case "KLDR":
                    kontakt.setInterstitiellEndokavitaer("K");
                    kontakt.setRateType("LDR");
                    result.setKontakt(kontakt);
                    break;
                default:
                    LOG.warn("Unknown radiation type: {}", applikationsart);
            }

            result.setKontakt(kontakt);
            return result;
        }

        if (applikationsart.startsWith("I")) {
            var kontakt = new Applikationsart.Kontakt();
            kontakt.setZielgebiet(zielgebiet);
            kontakt.setSeiteZielgebiet(seiteZielgebiet);
            kontakt.setEinzeldosis(einzeldosis);
            kontakt.setGesamtdosis(gesamtdosis);
            switch (applikationsart) {
                case "I":
                    result.setKontakt(kontakt);
                    break;
                case "IHDR":
                    kontakt.setInterstitiellEndokavitaer("I");
                    kontakt.setRateType("HDR");
                    result.setKontakt(kontakt);
                    break;
                case "IPDR":
                    kontakt.setInterstitiellEndokavitaer("I");
                    kontakt.setRateType("PDR");
                    result.setKontakt(kontakt);
                    break;
                case "ILDR":
                    kontakt.setInterstitiellEndokavitaer("I");
                    kontakt.setRateType("LDR");
                    result.setKontakt(kontakt);
                    break;
                default:
                    LOG.warn("Unknown radiation type: {}", applikationsart);
            }

            result.setKontakt(kontakt);
            return result;
        }

        if (applikationsart.startsWith("M")) {
            var metabolisch = new Applikationsart.Metabolisch();
            metabolisch.setZielgebiet(zielgebiet);
            metabolisch.setSeiteZielgebiet(seiteZielgebiet);
            switch (applikationsart) {
                case "M":
                    result.setMetabolisch(metabolisch);
                    break;
                case "MSIRT":
                    metabolisch.setMetabolischTyp("SIRT");
                    result.setMetabolisch(metabolisch);
                    break;
                case "MPRRT":
                    metabolisch.setMetabolischTyp("PRRT");
                    result.setMetabolisch(metabolisch);
                    break;
                default:
                    LOG.warn("Unknown radiation type: {}", applikationsart);
            }

            result.setMetabolisch(metabolisch);
            return result;
        }

        if (applikationsart.equals("S")) {
            var sonstige = new Applikationsart.Sonstige();
            sonstige.setZielgebiet(zielgebiet);
            sonstige.setSeiteZielgebiet(seiteZielgebiet);
            sonstige.setEinzeldosis(einzeldosis);
            sonstige.setGesamtdosis(gesamtdosis);
            result.setSonstige(sonstige);
            return result;
        }

        LOG.warn("Unexpected radiation type {}", applikationsart);

        return null;
    }
}
