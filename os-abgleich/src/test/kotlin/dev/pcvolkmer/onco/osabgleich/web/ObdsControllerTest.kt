package dev.pcvolkmer.onco.osabgleich.web

import dev.pcvolkmer.onco.osabgleich.MappingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.charset.Charset

@SpringJUnitWebConfig
@WebMvcTest(controllers = [ObdsController::class])
@AutoConfigureMockMvc
@MockitoBean(types = [MappingService::class])
class ObdsControllerTest {

    lateinit var mockMvc: MockMvc
    lateinit var mappingService: MappingService

    @BeforeEach
    fun setup(
        @Autowired mockMvc: MockMvc,
        @Autowired mappingService: MappingService
    ) {
        this.mockMvc = mockMvc
        this.mappingService = mappingService
    }

    @Test
    fun testShouldRequestObdsResponse() {
        doAnswer {
            ObdsControllerTest::class.java.getResourceAsStream("/testdaten/obdsv3.xml")
                ?.readAllBytes()
                ?.toString(Charset.defaultCharset())
        }.whenever(mappingService).get(any())

        mockMvc.get("/obds/{id}", 1)
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(MediaType.APPLICATION_XML)
                    xpath("/oBDS") { exists() }
                }
            }
    }

}