package dev.pcvolkmer.onco.osabgleich

import de.basisdatensatz.obds.v2.ADTGEKID
import de.basisdatensatz.obds.v2.ADTGEKID.MengePatient.Patient.MengeMeldung.Meldung.Diagnose
import de.basisdatensatz.obds.v2.SeitenlokalisationTyp
import dev.pcvolkmer.onko.obds2to3.ObdsMapper
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

@Service
class MappingService(
    private val databaseService: DatabaseService
) {

    private val mapper: ObdsMapper = ObdsMapper.builder().fixMissingId(false).ignoreUnmappable(true).build()

    private val cache: MutableMap<String, CacheObject> = mutableMapOf()

    fun map(adtString: String): MappingResult {
        val cacheId = DigestUtils.sha256Hex(adtString)
        val adt = mapper.readValue<ADTGEKID>(adtString, ADTGEKID::class.java)

        adt.mengePatient.patient.onEach { patient ->
            patient.mengeMeldung.meldung.onEach { meldung ->
                val result = databaseService.findByEinsendenummer(meldung.meldungID)

                if (null != result) {
                    val diagnose = meldung.diagnose ?: Diagnose()
                    if (diagnose.diagnosedatum.isNullOrBlank())
                        mapDate(result.diagnosedatum).ifPresent { date -> diagnose.diagnosedatum = date }
                    diagnose.tumorID = diagnose.tumorID ?: result.tumorId
                    diagnose.primaertumorICDCode = diagnose.primaertumorICDCode ?: result.icd10Code
                    diagnose.primaertumorICDVersion = diagnose.primaertumorICDVersion ?: result.icd10Version
                    diagnose.seitenlokalisation = try {
                        diagnose.seitenlokalisation ?: SeitenlokalisationTyp.fromValue(result.seite)
                    } catch (_: Exception) {
                        SeitenlokalisationTyp.U
                    }
                    diagnose.diagnosesicherung = result.diagnosesicherung
                    meldung.diagnose = diagnose
                }
            }
        }

        val obds = mapper.map(adt)
        cache[cacheId] = CacheObject(Instant.now(), mapper.writeXmlString(obds))

        return MappingResult(
            cacheId,
            adt.mengePatient.patient.size,
            obds.mengePatient.patient.size,
            adt.mengePatient.patient.flatMap { it.mengeMeldung.meldung }.size,
            obds.mengePatient.patient.flatMap { it.mengeMeldung.meldung }.size,
        )
    }

    fun get(id: String): String? {
        return cache[id]?.data
    }

    private fun mapDate(input: String): Optional<String> {
        val pattern = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})")
        val matcher = pattern.matcher(input)
        if (!matcher.find()) {
            return Optional.empty()
        }
        return Optional.of("${matcher.group("day")}.${matcher.group("month")}.${matcher.group("year")}")
    }

}

@JvmRecord
private data class CacheObject(val timestamp: Instant, val data: String)

@JvmRecord
data class MappingResult(
    val id: String,
    val patientsIn: Int,
    val patientsOut: Int,
    val messagesIn: Int,
    val messagesOut: Int
) {

    fun hasWarning() = patientsIn != patientsOut || messagesIn != messagesOut

}