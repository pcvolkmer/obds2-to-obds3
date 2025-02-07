package dev.pcvolkmer.onco.osabgleich

import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
class DatabaseService(
    private val jdbcTemplate: JdbcTemplate,
) {

    private var items = emptyList<DatabaseResult>()

    @PostConstruct
    fun update() {
        val sql = """
            SELECT einsendenummer, tumor_id, diagnosedatum, icd10_code, icd10_version, seite, diagnosesicherung FROM (
                   SELECT
                       einsendenummer,
                       tumoridentifikator AS tumor_id,
                       erkrankung.diagnosedatum,
                       erkrankung.icd10_code,
                       property_catalogue_version.lkr_code AS icd10_version,
                       erkrankung.seite,
                       dk_diagnose.diagnosesicherung,
                       COUNT(*) AS count
                   FROM dk_pathologie
                            JOIN prozedur ON (prozedur.id = dk_pathologie.id)
                            JOIN erkrankung_prozedur ON (erkrankung_prozedur.prozedur_id = prozedur.id)
                            JOIN erkrankung ON (erkrankung.id = erkrankung_prozedur.erkrankung_id)
                            JOIN erkrankung_prozedur ep2 ON (ep2.erkrankung_id = erkrankung.id)
                            JOIN dk_diagnose ON (dk_diagnose.id = ep2.prozedur_id)
                            JOIN patient ON (patient.id = erkrankung.patient_id)
                            JOIN property_catalogue_version ON (property_catalogue_version.id = erkrankung.icd10_version)
                   WHERE
                       einsendenummer IS NOT NULL
                     AND einsendenummer <> ''
                     AND einsendenummer NOT LIKE '%+%'
                     AND einsendenummer NOT LIKE '% %'
                   GROUP BY einsendenummer, patienten_id, tumoridentifikator
               ) sub
            WHERE COUNT = 1
            ORDER BY einsendenummer
        """.trimIndent()

        try {
            this.items = jdbcTemplate.query(sql, { rs: ResultSet, _: Int ->
                return@query DatabaseResult(
                    rs.getString("einsendenummer"),
                    rs.getString("tumor_id"),
                    rs.getString("diagnosedatum"),
                    rs.getString("icd10_code"),
                    rs.getString("icd10_version"),
                    rs.getString("seite"),
                    rs.getString("diagnosesicherung"),
                )
            })
        } catch (_: Exception) {
            // Nop
        }
    }

    fun findByEinsendenummer(einsendenummer: String): DatabaseResult? {
        return this.items.firstOrNull { it.einsendenummer == einsendenummer }
    }

}

data class DatabaseResult(
    val einsendenummer: String,
    val tumorId: String,
    val diagnosedatum: String,
    val icd10Code: String,
    val icd10Version: String,
    val seite: String,
    val diagnosesicherung: String,
)