package io.github.bzkf.obds2toobds3;

import de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeSYST.SYST.MengeNebenwirkung;
import de.basisdatensatz.obds.v3.NebenwirkungTyp;
import de.basisdatensatz.obds.v3.NebenwirkungTyp.MengeNebenwirkung.Nebenwirkung;
import de.basisdatensatz.obds.v3.SYSTTyp;
import de.basisdatensatz.obds.v3.SYSTTyp.EndeGrund;
import de.basisdatensatz.obds.v3.SYSTTyp.Meldeanlass;
import de.basisdatensatz.obds.v3.SYSTTyp.Therapieart;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemtherapieMapper {
  private static final Logger LOG = LoggerFactory.getLogger(SystemtherapieMapper.class);

  private SystemtherapieMapper() {}

  public static List<SYSTTyp> map(
      de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeSYST
          mengeSyst,
      String meldeanlass) {
    var result = new ArrayList<SYSTTyp>();
    for (var source : mengeSyst.getSYST()) {
      var systTyp = new SYSTTyp();

      systTyp.setSYSTID(source.getSYSTID());
      systTyp.setIntention(source.getSYSTIntention());
      systTyp.setStellungOP(source.getSYSTStellungOP());
      systTyp.setMeldeanlass(Meldeanlass.fromValue(meldeanlass));

      if (source.getMengeTherapieart() != null) {
        var therapieart = mapTherapieart(source.getMengeTherapieart().getSYSTTherapieart());
        systTyp.setTherapieart(therapieart);
      }

      systTyp.setProtokoll(source.getSYSTProtokoll());

      MapperUtils.mapDateStringGenau(source.getSYSTBeginnDatum()).ifPresent(systTyp::setBeginn);
      MapperUtils.mapDateString(source.getSYSTEndeDatum())
          .ifPresent(d -> systTyp.setEnde(d.getValue()));

      if (source.getMengeSubstanz() != null) {
        var mengeSubstanz = new SYSTTyp.MengeSubstanz();
        for (var substanz : source.getMengeSubstanz().getSYSTSubstanz()) {
          var substanzv3 = new SYSTTyp.MengeSubstanz.Substanz();
          substanzv3.setBezeichnung(substanz);
          mengeSubstanz.getSubstanz().add(substanzv3);
        }

        systTyp.setMengeSubstanz(mengeSubstanz);
      }

      if (source.getSYSTEndeGrund() != null) {
        systTyp.setEndeGrund(EndeGrund.fromValue(source.getSYSTEndeGrund()));
      }

      if (source.getMengeNebenwirkung() != null) {
        var mappedNebenwirkungen = mapNebenwirkungen(source.getMengeNebenwirkung());
        systTyp.setNebenwirkungen(mappedNebenwirkungen);
      }

      result.add(systTyp);
    }

    return result;
  }

  private static Therapieart mapTherapieart(List<String> therapieartV2) {
    // v2: CH, HO, IM, KM, WS, AS, ZS, SO
    // v3: CH, HO, IM, ??, WS, AS, ZS, SO, CI, CZ, CIZ, IZ, SZ, WW
    // v3: SZ = Stammzelltransplantation (inkl. Knochenmarktransplantation)

    if (therapieartV2.size() == 1) {
      var code = therapieartV2.get(0);
      if (code.equals("KM")) {
        return Therapieart.SZ;
      }
      return Therapieart.fromValue(code);
    }

    var sortedCodes = new ArrayList<String>(therapieartV2);
    Collections.sort(sortedCodes);
    var code = String.join("", sortedCodes);

    var extraMappings =
        Map.of(
            "CHIMSO", Therapieart.CI,
            "CHSO", Therapieart.CH);

    if (extraMappings.containsKey(code)) {
      LOG.warn(
          "Therapieart '{}' not directly mappable. Falling back to '{}'",
          code,
          extraMappings.get(code));
      return extraMappings.get(code);
    }

    return switch (code) {
      case "CHIM" -> Therapieart.CI;
      case "CHZS" -> Therapieart.CZ;
      case "CHIMZS" -> Therapieart.CIZ;
      case "IMZS" -> Therapieart.IZ;
      default -> throw new IllegalStateException("Unsupported Therapieart: " + code);
    };
  }

  // painfully similar to the one in "StrahlentherapieMapper", but the types are
  // unfortunately different.
  private static NebenwirkungTyp mapNebenwirkungen(MengeNebenwirkung mengeNebenwirkung) {
    var mengeNebenwirkungV3 = new de.basisdatensatz.obds.v3.NebenwirkungTyp.MengeNebenwirkung();

    var result = new NebenwirkungTyp();

    for (var nebenwirkungV2 : mengeNebenwirkung.getSYSTNebenwirkung()) {
      var nebenwirkungV3 = new Nebenwirkung();

      if (nebenwirkungV2.getNebenwirkungGrad() != null
          && (nebenwirkungV2.getNebenwirkungGrad().equals("K")
              || nebenwirkungV2.getNebenwirkungGrad().equals("U"))) {
        if (mengeNebenwirkung.getSYSTNebenwirkung().size() > 1) {
          LOG.warn(
              "SYST has multiple Nebenwirkungen, but should only contain one if set to 'K' or 'U'.");
        }

        if (nebenwirkungV2.getNebenwirkungArt() != null) {
          LOG.warn(
              "Nebenwirkung Grad is 'K' or 'U', but the Art is set. This information is lost after the mapping.");
        }

        result.setGradMaximal2OderUnbekannt(nebenwirkungV2.getNebenwirkungGrad());
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
}
