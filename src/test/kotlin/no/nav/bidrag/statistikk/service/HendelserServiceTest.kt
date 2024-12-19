package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.bo.ForskuddHendelse
import no.nav.bidrag.statistikk.bo.ForskuddPeriode
import no.nav.bidrag.statistikk.bo.Inntekt
import no.nav.bidrag.statistikk.hendelse.StatistikkKafkaEventProducer
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettEngangsbeløpRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("HendelserServiceTest")
@ActiveProfiles(BidragStatistikkTest.TEST_PROFILE)
@SpringBootTest(classes = [BidragStatistikkTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class HendelserServiceTest {

    @Autowired
    private lateinit var hendelserService: HendelserService

    @MockBean
    private lateinit var vedtakEventProducerMock: StatistikkKafkaEventProducer

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette hendelse`() {
        hendelserService.opprettHendelse(
            ForskuddHendelse(
                vedtaksid = 1,
                vedtakstidspunkt = LocalDateTime.now(),
                type = Vedtakstype.ENDRING.name,
                kravhaver = "12345",
                mottaker = "54321",
                forskuddPeriodeListe = listOf(
                    ForskuddPeriode(
                        periodeFra = LocalDate.of(2021, 1, 1),
                        periodeTil = LocalDate.of(2022, 1, 1),
                        beløp = BigDecimal(1000),
                        resultat = Beslutningstype.ENDRING.name,
                        barnetsAldersgruppe = "0-6",
                        antallBarnIEgenHusstand = 1.0,
                        sivilstand = "ENKE",
                        barnBorMedBM = true,
                        inntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name
                            )
                        )
                    )
                )
            )
        )

        verify(vedtakEventProducerMock).publish(anyOrNull())
    }


}
