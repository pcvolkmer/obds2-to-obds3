package dev.pcvolkmer.onco.osabgleich.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Controller
class HomeController {

    @GetMapping("/")
    fun home(): String {
        return "home"
    }

    @PostMapping("/")
    fun upload(): String {
        return "upload"
    }

}