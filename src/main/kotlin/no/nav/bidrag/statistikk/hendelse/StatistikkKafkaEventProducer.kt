package no.nav.bidrag.statistikk.hendelse

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.statistikk.bo.ForskuddHendelse
import org.springframework.kafka.core.KafkaTemplate

interface StatistikkKafkaEventProducer {
    fun publish(forskuddHendelse: ForskuddHendelse)
}

class DefaultStatistikkKafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String?, String?>?,
    private val objectMapper: ObjectMapper,
    private val topic: String,
) : StatistikkKafkaEventProducer {

    override fun publish(forskuddHendelse: ForskuddHendelse) {
        try {
            kafkaTemplate?.send(
                topic,
                forskuddHendelse.vedtaksid.toString(),
                objectMapper.writeValueAsString(forskuddHendelse),
            )?.get()
        } catch (e: JsonProcessingException) {
            throw IllegalStateException(e.message, e)
        }
    }
}
