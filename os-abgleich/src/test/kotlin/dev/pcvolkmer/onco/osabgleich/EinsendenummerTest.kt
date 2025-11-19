package dev.pcvolkmer.onco.osabgleich

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EinsendenummerTest {

    @ParameterizedTest
    @CsvSource(value = [
        "A25/001234,A/2025/001234",
        "A25/1234,A/2025/001234",
        "A25/1234.100,A/2025/001234",
        "A/25/001234,A/2025/001234",
        "A/25/1234,A/2025/001234",
        "A/25/1234.100,A/2025/001234",
        "A/2025/001234,A/2025/001234",
        "A/2025/1234,A/2025/001234",
        "A/2025/1234.100,A/2025/001234",
        " A25/1234.100,A/2025/001234",
        "\tA25/1234.100,A/2025/001234",
    ])
    fun testShouldNormalizeValue(input: String, expected: String) {
        assertThat(Einsendenummer.from(input)).isEqualTo(Einsendenummer.from(expected))
    }

}
