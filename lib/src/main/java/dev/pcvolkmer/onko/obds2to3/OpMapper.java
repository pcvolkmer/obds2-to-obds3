package dev.pcvolkmer.onko.obds2to3;

import java.util.List;

import de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeOP.OP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.basisdatensatz.obds.v3.OPTyp;
import de.basisdatensatz.obds.v3.RTyp;
import de.basisdatensatz.obds.v3.OPTyp.Komplikationen;
import de.basisdatensatz.obds.v3.OPTyp.MengeOPS;
import de.basisdatensatz.obds.v3.OPTyp.MengeOPS.OPS;

public class OpMapper {
    private static final Logger LOG = LoggerFactory.getLogger(OpMapper.class);

    private OpMapper() { }

    public static List<OPTyp> map(
            de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.MengeOP mengeOp) {
        return mengeOp.getOP().stream().map(OpMapper::mapToOPType).toList();
    }

    private static OPTyp mapToOPType(OP source) {
        var opTyp = new OPTyp();
        opTyp.setOPID(source.getOPID());
        opTyp.setIntention(source.getOPIntention());

        MapperUtils.mapDateString(source.getOPDatum()).ifPresent(d -> opTyp.setDatum(d.getValue()));

        if (source.getMengeOPS() != null) {
            var mengeOPS = new MengeOPS();
            for (var ops : source.getMengeOPS().getOPOPS()) {
                var opsv3 = new OPS();
                opsv3.setCode(ops);
                opsv3.setVersion(source.getOPOPSVersion());

                mengeOPS.getOPS().add(opsv3);
            }

            opTyp.setMengeOPS(mengeOPS);
        }

        MeldungMapper.mapHistologie(source.getHistologie()).ifPresent(opTyp::setHistologie);
        MeldungMapper.mapTnmType(source.getTNM()).ifPresent(opTyp::setTNM);

        if (source.getMengeKomplikation() != null) {
            opTyp.setKomplikationen(mapToKomplikationen(source));
        }

        // v2: R0, R1, R1(is), R1(cy+), R2, RX
        // v3: R0, R1, R1(is), R1(cy+), R2, RX, U
        if (source.getResidualstatus() != null) {
            var residualstatus = new de.basisdatensatz.obds.v3.ResidualstatusTyp();

            var gesamtbeurteilung = source.getResidualstatus().getGesamtbeurteilungResidualstatus();
            if (gesamtbeurteilung != null) {
                residualstatus.setGesamtbeurteilungResidualstatus(RTyp.fromValue(gesamtbeurteilung.value()));
            } else {
                LOG.debug("OP {} has no GesamtbeurteilungResidualstatus set in v2 residualstatus", source.getOPID());
            }

            var lokaleBeurteilung = source.getResidualstatus().getLokaleBeurteilungResidualstatus();
            if (lokaleBeurteilung != null) {
                residualstatus.setLokaleBeurteilungResidualstatus(RTyp.fromValue(lokaleBeurteilung.value()));
            } else {
                LOG.debug("OP {} has no LokaleBeurteilungResidualstatus set in v2 residualstatus", source.getOPID());
            }

            opTyp.setResidualstatus(residualstatus);
        }

        if (source.getModulProstata() != null) {
            opTyp.setModulProstata(ModulMapper.map(source.getModulProstata()));
        }

        return opTyp;
    }

    private static Komplikationen mapToKomplikationen(OP source) {
        var komplikationen = new Komplikationen();
        for (var komp : source.getMengeKomplikation().getOPKomplikation()) {
            if (komp.equals("N") || komp.equals("U")) {
                komplikationen.setKomplikationNeinOderUnbekannt(komp);
                if (source.getMengeKomplikation().getOPKomplikation().size() > 1) {
                    LOG.warn(
                            "OP {} has multiple Komplikationen, but should only contain one if set to 'N' or 'U'.",
                            source.getOPID());
                }
            } else {
                var komplikationTyp = new de.basisdatensatz.obds.v3.KomplikationTyp();
                komplikationTyp.setKuerzel(komp);
                komplikationen.getMengeKomplikation().getKomplikation().add(komplikationTyp);
            }
        }

        return komplikationen;
    }
}
