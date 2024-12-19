package no.nav.bidrag.statistikk.service

import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.bo.ForskuddHendelse
import no.nav.bidrag.statistikk.hendelse.StatistikkKafkaEventProducer
import no.nav.bidrag.statistikk.util.StatistikkUtil.Companion.tilJson
import org.springframework.stereotype.Service

@Service
class HendelserService(private val statistikkKafkaEventProducer: StatistikkKafkaEventProducer) {

    fun opprettHendelse(forskuddHendelse: ForskuddHendelse) {
        statistikkKafkaEventProducer.publish(forskuddHendelse)
        SECURE_LOGGER.info("Ny melding lagt på topic statistikk: ${tilJson(forskuddHendelse)}")
    }
}
