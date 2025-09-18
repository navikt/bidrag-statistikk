package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.bo.BidragHendelse
import no.nav.bidrag.statistikk.bo.BidragPeriode
import no.nav.bidrag.statistikk.hendelse.StatistikkKafkaEventProducer
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import no.nav.bidrag.transport.behandling.statistikk.ForskuddPeriode
import no.nav.bidrag.transport.behandling.statistikk.Inntekt
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
    private lateinit var statistikkEventProducerMock: StatistikkKafkaEventProducer

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette forskuddshendelse`() {
        hendelserService.opprettForskuddshendelse(
            ForskuddHendelse(
                vedtaksid = 1,
                vedtakstidspunkt = LocalDateTime.now(),
                type = Vedtakstype.ENDRING.name,
                saksnr = "123",
                kravhaver = "12345",
                mottaker = "54321",
                historiskVedtak = false,
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
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                    ),
                ),
            ),
        )

        verify(statistikkEventProducerMock).publishForskudd(anyOrNull())
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette bidragshendelse`() {
        hendelserService.opprettBidragshendelse(
            BidragHendelse(
                vedtaksid = 1,
                vedtakstidspunkt = LocalDateTime.now(),
                stønadstype = Stønadstype.BIDRAG,
                type = Vedtakstype.ENDRING.name,
                saksnr = "123",
                skyldner = "23456",
                kravhaver = "12345",
                mottaker = "54321",
                historiskVedtak = false,
                innkreving = true,
                bidragPeriodeListe = listOf(
                    BidragPeriode(
                        periodeFra = LocalDate.of(2021, 1, 1),
                        periodeTil = LocalDate.of(2022, 1, 1),
                        beløp = BigDecimal(1000),
                        valutakode = "NOK",
                        resultat = Beslutningstype.ENDRING.name,
                        bidragsevne = BigDecimal(1000),
                        underholdskostnad = BigDecimal(1000),
                        bPsAndelUnderholdskostnad = BigDecimal(1000),
                        nettoTilsynsutgift = BigDecimal(1000),
                        faktiskUtgift = BigDecimal(1000),
                        samværsfradrag = BigDecimal(1000),
                        nettoBarnetilleggBP = BigDecimal(1000),
                        nettoBarnetilleggBM = BigDecimal(1000),
                        bPBorMedAndreVoksne = true,
                        samværsklasse = Samværsklasse.DELT_BOSTED,
                        bPInntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                        bMInntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                    ),
                ),
            ),
        )

        verify(statistikkEventProducerMock).publishBidrag(anyOrNull())
    }
}
