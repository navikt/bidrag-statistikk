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

    fun publishForskudd(forskuddHendelse: ForskuddHendelse): Long? {
        val headers = listOf(RecordHeader("type", "FORSKUDD".toByteArray()))
        val record = ProducerRecord(
            topic,
            null,
            forskuddHendelse.vedtaksid.toString() + forskuddHendelse.kravhaver,
            objectMapper.writeValueAsString(forskuddHendelse),
            headers,
        )

        try {
            val offset = kafkaTemplate.send(
                record,
            ).get().recordMetadata.offset()
            return offset
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved sending av kafkamelding, $record" }
            throw IllegalStateException(e.message, e)
        }
    }
}
