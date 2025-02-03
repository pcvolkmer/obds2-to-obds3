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

@SpringJUnitWebConfig
@WebMvcTest(controllers = [ObdsController::class])
@AutoConfigureMockMvc
class ObdsControllerTest {

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup(
        @Autowired mockMvc: MockMvc,
    ) {
        this.mockMvc = mockMvc
    }

    @Test
    fun testShouldRequestObdsResponse() {
        mockMvc.get("/obds/{id}", 1)
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(MediaType.APPLICATION_XML)
                    xpath("/OBDS") { exists() }
                }
            }

    }

}