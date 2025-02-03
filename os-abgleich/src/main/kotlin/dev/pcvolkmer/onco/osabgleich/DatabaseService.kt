package dev.pcvolkmer.onco.osabgleich

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DatabaseService(
    private val jdbcTemplate: JdbcTemplate,
) {

    fun findByEinsendenummer(einsendenummer: String) {
        val sql = """
            SELECT * FROM (
                  SELECT
                        einsendenummer,
                        patienten_id,
                        tumoridentifikator,
                        erkrankung.diagnosedatum,
                        erkrankung.icd10_code,
                        property_catalogue_version.lkr_code,
                        COUNT(*) AS count
                  FROM dk_pathologie
                        JOIN prozedur ON (prozedur.id = dk_pathologie.id)
                        JOIN erkrankung_prozedur ON (erkrankung_prozedur.prozedur_id = prozedur.id)
                        JOIN erkrankung ON (erkrankung.id = erkrankung_prozedur.erkrankung_id)
                        JOIN patient ON (patient.id = erkrankung.patient_id)
                        JOIN property_catalogue_version ON (property_catalogue_version.id = erkrankung.icd10_version)
                  WHERE
                        einsendenummer IS NOT NULL
                        AND einsendenummer <> ''
                        AND einsendenummer NOT LIKE '%+%'
                        AND einsendenummer NOT LIKE '% %'
                        AND durchfuehrendeoe_fachabteilung = 19
                  GROUP BY einsendenummer, patienten_id, tumoridentifikator
            ) sub
                  WHERE COUNT > 0
                  ORDER BY einsendenummer;
        """.trimIndent()

        jdbcTemplate.queryForObject(sql, DatabaseResult::class.java)
    }

}

data class DatabaseResult(val einsendenummer: String, val tumorId: String, val diagnosedatum: Instant, val icd10Code: String, val icd10Version: String)