package no.nav.bidrag.statistikk.service

import no.nav.bidrag.statistikk.LOGGER
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.hendelse.StatistikkKafkaEventProducer
import no.nav.bidrag.statistikk.util.StatistikkUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import org.springframework.stereotype.Service

@Service
class HendelserService(private val statistikkKafkaEventProducer: StatistikkKafkaEventProducer) {

    fun opprettHendelse(forskuddHendelse: ForskuddHendelse) {
        val offset = statistikkKafkaEventProducer.publishForskudd(forskuddHendelse)
        LOGGER.info("Ny melding lagt på topic bidrag.statistikk med offset: $offset og vedtaksid: ${forskuddHendelse.vedtaksid}")
        SECURE_LOGGER.info("Ny melding lagt på topic bidrag.statistikk med offset: $offset og hendelse : ${tilJson(forskuddHendelse)}")
    }
}
