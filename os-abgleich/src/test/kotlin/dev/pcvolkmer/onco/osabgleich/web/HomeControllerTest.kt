package dev.pcvolkmer.onco.osabgleich.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringJUnitWebConfig
@WebMvcTest(controllers = [HomeController::class])
@AutoConfigureMockMvc
class HomeControllerTest {

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup(
        @Autowired mockMvc: MockMvc,
    ) {
        this.mockMvc = mockMvc
    }

    @Test
    fun testShouldRequestHome() {
        mockMvc.get("/").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
        }
    }

    @Test
    fun testShouldRequestUpload() {
        mockMvc.post("/").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
        }
    }

}