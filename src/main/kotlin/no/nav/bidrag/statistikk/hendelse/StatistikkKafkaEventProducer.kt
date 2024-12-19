package no.nav.bidrag.statistikk.hendelse

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.kafka.core.KafkaTemplate

interface StatistikkKafkaEventProducer {
    fun publishForskudd(forskuddHendelse: ForskuddHendelse)
}

class DefaultStatistikkKafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String?, String?>?,
    private val objectMapper: ObjectMapper,
    private val topic: String,
) : StatistikkKafkaEventProducer {

    override fun publishForskudd(forskuddHendelse: ForskuddHendelse) {

        val headers = listOf(RecordHeader("type", "FORSKUDD".toByteArray()))
        val record = ProducerRecord(topic, null, forskuddHendelse.vedtaksid.toString(), objectMapper.writeValueAsString(forskuddHendelse), headers)

        try {
            kafkaTemplate?.send(
                record
            )?.get()
        } catch (e: JsonProcessingException) {
            throw IllegalStateException(e.message, e)
        }
    }
}
