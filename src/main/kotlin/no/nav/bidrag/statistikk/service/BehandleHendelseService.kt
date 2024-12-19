package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = LoggerFactory.getLogger(DefaultBehandleHendelseService::class.java)

interface BehandleHendelseService {
    fun behandleHendelse(vedtakHendelse: VedtakHendelse)
}

@Service
@Transactional
class DefaultBehandleHendelseService(private val statistikkService: StatistikkService) :
    BehandleHendelseService {
    override fun behandleHendelse(vedtakHendelse: VedtakHendelse) {
        SECURE_LOGGER.info("Behandler vedtakHendelse: $vedtakHendelse")

        if (vedtakstypeTilStatikk(vedtakHendelse.type)) {
            statistikkService.behandleVedtakshendelse(vedtakHendelse)
        }
    }

    private fun vedtakstypeTilStatikk(vedtakstype: Vedtakstype): Boolean {
        return vedtakstype == Vedtakstype.ENDRING || vedtakstype == Vedtakstype.FASTSETTELSE
    }
}
