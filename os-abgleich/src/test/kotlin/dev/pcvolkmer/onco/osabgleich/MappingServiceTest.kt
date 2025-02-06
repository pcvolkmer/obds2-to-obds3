package dev.pcvolkmer.onco.osabgleich

import dev.pcvolkmer.onco.osabgleich.web.ObdsControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.nio.charset.Charset

@ExtendWith(MockitoExtension::class)
class MappingServiceTest {

    private lateinit var databaseService: DatabaseService
    private lateinit var mappingService: MappingService

    @BeforeEach
    fun setUp(
        @Mock databaseService: DatabaseService
    ) {
        this.databaseService = databaseService
        this.mappingService = MappingService(databaseService)
    }

    @Test
    fun testShouldMapInputForEinsendenummerInDatabase() {
        // Mock database request
        doAnswer {
            val einsendenummer = it.getArgument<String>(0)
            return@doAnswer when (einsendenummer) {
                "T/2024/000001" -> DatabaseResult(einsendenummer, "1", "2023-07-01", "C00.0", "10 2022 GM", "L", "2")
                else -> null
            }
        }.whenever(databaseService).findByEinsendenummer(any())

        val obds2Content = ObdsControllerTest::class.java.getResourceAsStream("/testdaten/obdsv2.xml")
            ?.readAllBytes()
            ?.toString(Charset.defaultCharset())

        val obds3Content = ObdsControllerTest::class.java.getResourceAsStream("/testdaten/obdsv3.xml")
            ?.readAllBytes()
            ?.toString(Charset.defaultCharset())

        val result = this.mappingService.map(obds2Content!!)

        assertThat(this.mappingService.get(result.id)).isEqualTo(obds3Content)
    }

    @Test
    fun testShouldNotMapInputForMissingBitRequiredInformation() {
        // Mock database request
        doAnswer {
            val einsendenummer = it.getArgument<String>(0)
            return@doAnswer when (einsendenummer) {
                "T/2024/000001" -> DatabaseResult(einsendenummer, "1", "2023-07-01", "C00.0", "10 2022 GM", "L", "2")
                "T/2024/000002" -> DatabaseResult(einsendenummer, "1", "2023-07-01", "C00.0", "10 2022 GM", "", "")
                else -> null
            }
        }.whenever(databaseService).findByEinsendenummer(any())

        val obds2Content = ObdsControllerTest::class.java.getResourceAsStream("/testdaten/obdsv2.xml")
            ?.readAllBytes()
            ?.toString(Charset.defaultCharset())

        val obds3Content = ObdsControllerTest::class.java.getResourceAsStream("/testdaten/obdsv3.xml")
            ?.readAllBytes()
            ?.toString(Charset.defaultCharset())

        val result = this.mappingService.map(obds2Content!!)

        assertThat(this.mappingService.get(result.id)).isEqualTo(obds3Content)
    }

}