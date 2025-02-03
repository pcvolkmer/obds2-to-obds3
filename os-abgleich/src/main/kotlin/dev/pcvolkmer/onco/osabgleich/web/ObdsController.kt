package dev.pcvolkmer.onco.osabgleich.web

import de.basisdatensatz.obds.v3.OBDS
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ObdsController {

    @GetMapping( path = ["/obds/{id}"], produces = [MediaType.APPLICATION_XML_VALUE])
    fun requestObds3(): ResponseEntity<OBDS> {
        return ResponseEntity.ok(OBDS())
    }

}