package no.nav.bidrag.statistikk.hendelse

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
private val LOGGER = KotlinLogging.logger {}

@Service
class StatistikkKafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${TOPIC_STATISTIKK}") private val topic: String,
) {

    fun publishForskudd(forskuddHendelse: ForskuddHendelse) {
        val headers = listOf(RecordHeader("type", "FORSKUDD".toByteArray()))
        val record = ProducerRecord(topic, null, forskuddHendelse.vedtaksid.toString(), objectMapper.writeValueAsString(forskuddHendelse), headers)

        try {
            kafkaTemplate.send(
                record,
            ).get()
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved sending av kafka melding, $record" }
            throw IllegalStateException(e.message, e)
        }
    }
}
