package dev.pcvolkmer.onco.osabgleich.web

import dev.pcvolkmer.onco.osabgleich.MappingService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ObdsController(
    private var mappingService: MappingService
) {

    @GetMapping( path = ["/obds/{id}"], produces = [MediaType.APPLICATION_XML_VALUE])
    fun requestObds3(@PathVariable id: String): ResponseEntity<String> {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$id.xml")
            .body(mappingService.get(id))
    }

}
