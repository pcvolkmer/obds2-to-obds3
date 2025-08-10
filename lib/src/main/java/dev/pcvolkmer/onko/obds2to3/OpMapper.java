package dev.pcvolkmer.onko.obds2to3;

import java.util.ArrayList;
import java.util.List;
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
        var result = new ArrayList<OPTyp>();
        for (var source : mengeOp.getOP()) {
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

                opTyp.setKomplikationen(komplikationen);
            }

            // v2: R0, R1, R1(is), R1(cy+), R2, RX
            // v3: R0, R1, R1(is), R1(cy+), R2, RX, U
            if (source.getResidualstatus() != null) {
                var residualstatus = new de.basisdatensatz.obds.v3.ResidualstatusTyp();
                residualstatus.setGesamtbeurteilungResidualstatus(
                        RTyp.fromValue(source.getResidualstatus().getGesamtbeurteilungResidualstatus().value()));
                residualstatus.setLokaleBeurteilungResidualstatus(
                        RTyp.fromValue(source.getResidualstatus().getLokaleBeurteilungResidualstatus().value()));
                opTyp.setResidualstatus(residualstatus);
            }

            if (source.getModulProstata() != null) {
                opTyp.setModulProstata(ModulMapper.map(source.getModulProstata()));
            }

            result.add(opTyp);
        }

        return result;
    }
}
