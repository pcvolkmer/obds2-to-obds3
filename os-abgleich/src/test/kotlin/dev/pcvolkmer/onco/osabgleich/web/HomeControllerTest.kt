package dev.pcvolkmer.onco.osabgleich.web

import dev.pcvolkmer.onco.osabgleich.MappingService
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doAnswer
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart

@SpringJUnitWebConfig
@ExtendWith(MockitoExtension::class)
@WebMvcTest(controllers = [HomeController::class])
@AutoConfigureMockMvc
@MockitoBean(types = [MappingService::class])
class HomeControllerTest {

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
    fun testShouldRequestHome() {
        mockMvc.get("/").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
            view { name("home") }
        }
    }

    @Test
    fun testShouldUploadWithResponse() {
        doAnswer {
            "1234567890"
        }.whenever(mappingService).map(any())

        val file = MockMultipartFile(
            "file",
            "file",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            HomeControllerTest::class.java.getResourceAsStream("/testdaten/obdsv2.xml")!!
        )

        mockMvc.multipart("/") {
            file(file)
            header("HX-Request", "true")
        }.andExpect {
            status { isOk() }
            content {
                contentTypeCompatibleWith(MediaType.TEXT_HTML)
                string(containsString("<a href=\"/obds/1234567890\">Anfrage 1234567890 herunterladen</a>"))
            }
        }
    }

    @Test
    fun testShouldUploadWithMappingError() {
        whenever(mappingService.map(any())).thenThrow(RuntimeException("Test exception"))

        val file = MockMultipartFile(
            "file",
            "file",
            MediaType.MULTIPART_FORM_DATA_VALUE,
            HomeControllerTest::class.java.getResourceAsStream("/testdaten/obdsv2.xml")!!
        )

        mockMvc.multipart("/") {
            file(file)
            header("HX-Request", "true")
        }.andExpect {
            status { isOk() }
            content {
                contentTypeCompatibleWith(MediaType.TEXT_HTML)
                string(containsString("<div>Test exception</div>"))
            }
        }
    }

}