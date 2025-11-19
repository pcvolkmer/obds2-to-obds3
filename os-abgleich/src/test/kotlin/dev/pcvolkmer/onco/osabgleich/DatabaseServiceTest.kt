package dev.pcvolkmer.onco.osabgleich

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

@ExtendWith(MockitoExtension::class)
class DatabaseServiceTest {

    private lateinit var service: DatabaseService

    @BeforeEach
    fun setUp(
        @Mock jdbcTemplate: JdbcTemplate
    ) {
        this.service = DatabaseService(jdbcTemplate)

        doAnswer {
            listOf(
                DatabaseResult("\tA/2025/1234", "1", "2024-07-01", "C00.0", "10 2022 GM", "L", "2"),
                DatabaseResult("\tA25/7654", "2", "2024-01-01", "C00.0", "10 2022 GM", "L", "2")
            )
        }.whenever(jdbcTemplate).query(anyString(), any<RowMapper<*>>())

        this.service.update()
    }

    @ParameterizedTest
    @CsvSource(value = [
        "A/2025/001234",
        "A/2025/007654",
    ])
    fun shouldMap(input: String) {
        val actual = this.service.findByEinsendenummer(input)
        assertThat(actual?.einsendenummer).isNotNull()
    }

}
