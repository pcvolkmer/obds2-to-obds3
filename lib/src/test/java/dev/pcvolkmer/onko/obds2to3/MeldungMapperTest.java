package dev.pcvolkmer.onko.obds2to3;

import de.basisdatensatz.obds.v2.ADTGEKID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeldungMapperTest {

    MeldungMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new MeldungMapper(false, false);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   "
    })
    void shouldThrowExceptionOnMissingMeldungID(String meldungID) {
        var meldung = new ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung();
        meldung.setMeldungID(meldungID);
        var ex = assertThrows(IllegalArgumentException.class, () -> mapper.map(meldung));
        assertThat(ex).hasMessage("MeldungID must not be null or blank");
    }

}
