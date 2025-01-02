package no.nav.bidrag.stønad.konfig

import no.nav.bidrag.statistikk.SECURE_LOGGER
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener

class KafkaRetryListener : RetryListener {
    override fun failedDelivery(record: ConsumerRecord<*, *>, exception: Exception, deliveryAttempt: Int) {
        SECURE_LOGGER.error("Håndtering av kafkamelding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk", exception)
    }

    override fun recovered(record: ConsumerRecord<*, *>, exception: java.lang.Exception) {
        SECURE_LOGGER.error(
            "Håndtering av kafkamelding ${record.value()} er enten suksess eller ignorert på grunn av ugyldig data",
            exception,
        )
    }

    override fun recoveryFailed(record: ConsumerRecord<*, *>, original: java.lang.Exception, failure: java.lang.Exception) {
    }
}
