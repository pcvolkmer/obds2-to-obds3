package dev.pcvolkmer.onco.osabgleich.web

import dev.pcvolkmer.onco.osabgleich.MappingService
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@Controller
class HomeController(
    private val mappingService: MappingService
) {

    @GetMapping("/")
    fun home(): String {
        return "home"
    }

    @HxRequest
    @PostMapping("/")
    fun upload(@RequestParam("file") file: MultipartFile, model: Model): String {
        try {
            val id = mappingService.map(file.bytes.decodeToString())
            model.addAttribute("id", id)
            return "upload"
        } catch (e: Exception) {
            model.addAttribute("error", e.message)
            return "upload"
        }
    }

}