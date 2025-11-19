package io.github.bzkf.obds2toobds3;

import static org.assertj.core.api.Assertions.assertThat;

import de.basisdatensatz.obds.v2.ADTGEKID;
import de.basisdatensatz.obds.v3.SYSTTyp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SystemtherapieMapperTest {

  @ParameterizedTest
  @CsvSource({
    "CH,CH",
    "HO,HO",
    "IM,IM",
    "WS,WS",
    "AS,AS",
    "ZS,ZS",
    "SO,SO",
    "CH;IM,CI",
    "CH;ZS,CZ",
    "CH;IM;ZS,CIZ",
    "IM;ZS,IZ",
    // Not direct mappings
    "KM,SZ",
    "CH;IM;SO,CI",
    "CH;SO,CH"
  })
  void shouldMapTherapieart(String in, String out) {
    var menge = new ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeSYST();
    var syst = new ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeSYST.SYST();
    menge.getSYST().add(syst);

    var mengeTherapieart =
        new ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeSYST.SYST.MengeTherapieart();
    for (var art : in.split(";")) {
      mengeTherapieart.getSYSTTherapieart().add(art);
    }
    syst.setMengeTherapieart(mengeTherapieart);

    var actual = SystemtherapieMapper.map(menge, SYSTTyp.Meldeanlass.BEHANDLUNGSENDE.value());

    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).getTherapieart()).isEqualTo(SYSTTyp.Therapieart.fromValue(out));
  }
}
