package dev.pcvolkmer.onco.osabgleich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.util.DigestUtils
import java.lang.reflect.Method

@SpringBootApplication
class OsAbgleichApplication {

    @Bean
    fun keyGenerator(): KeyGenerator {
        return SimpleKeyGenerator()
    }

}

fun main(args: Array<String>) {
    runApplication<OsAbgleichApplication>(*args)
}
