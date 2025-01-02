package no.nav.bidrag.statistikk.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.text.SimpleDateFormat

open class StatistikkUtil {

    companion object {
        fun tilJson(json: Any): String {
            val objectMapper = ObjectMapper()
            objectMapper.registerKotlinModule()
            objectMapper.writerWithDefaultPrettyPrinter()
            objectMapper.registerModule(JavaTimeModule())
            objectMapper.dateFormat = SimpleDateFormat("yyyy-MM-dd")
            return objectMapper.writeValueAsString(json)
        }
    }
}
