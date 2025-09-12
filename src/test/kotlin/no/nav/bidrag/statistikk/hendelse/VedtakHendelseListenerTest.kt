package no.nav.bidrag.statistikk.hendelse

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.BidragStatistikkTest.Companion.TEST_PROFILE
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoAldersjusteringBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoBidragUtenGrunnlag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoForskudd
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoUtenForskuddOgBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoUtenGrunnlag
import no.nav.bidrag.statistikk.TestUtil.Companion.lesVedtakDtoFraFil
import no.nav.bidrag.statistikk.TestUtil.Companion.stubHenteVedtak
import no.nav.bidrag.statistikk.bo.BidragHendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

@SpringBootTest(classes = [BidragStatistikkTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("VedtakHendelseListener (test av forretningslogikk)")
@ActiveProfiles(TEST_PROFILE)
@EnableMockOAuth2Server
@EnableAspectJAutoProxy
@AutoConfigureWireMock(port = 0)
class VedtakHendelseListenerTest {
    @Autowired
    private lateinit var vedtakHendelseListener: VedtakHendelseListener

    @MockkBean(relaxed = true)
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockBean
    private lateinit var statistikkKafkaEventProducerMock: StatistikkKafkaEventProducer

    @BeforeEach
    fun init() {
        every { kafkaTemplate.send(any<ProducerRecord<String, String>>()) } returns CompletableFuture.completedFuture(SendResult(null, null))
        every { kafkaTemplate.afterSingletonsInstantiated() } returns Unit
    }

    @Test
    fun `skal lese vedtakshendelse Forskudd uten feil`() {
        stubHenteVedtak(byggVedtakDtoForskudd())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"99999999",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                },
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
              }                       
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(2)).publishForskudd(anyOrNull())
    }

    @Test
    fun `skal lese vedtakshendelse Forskudd uten grunnlag og sjekke at det fortsatt produseres hendelse`() {
        stubHenteVedtak(byggVedtakDtoUtenGrunnlag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"99999999",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                },
                {
                 "type": "BIDRAG",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }                         
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishForskudd(anyOrNull())
    }

    @Test
    fun `skal ikke behandle hendelse Forskudd hvis vedtak ikke inneholder forskudd eller bidrag`() {
        stubHenteVedtak(byggVedtakDtoUtenForskuddOgBidrag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"1",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verifyNoInteractions(statistikkKafkaEventProducerMock)
    }

    @Test
    fun `skal lese vedtakshendelse Bidrag uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidrag("bidrag-behandling-q2"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"99999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "UTEN_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(99999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-01T23:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 2)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(100))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(3500))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[0].bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(400))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBP).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].bPBorMedAndreVoksne).isTrue
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.DELT_BOSTED)
        assertThat(hendelser[0].bidragPeriodeListe[0].bPInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(1000))
        assertThat(hendelser[0].bidragPeriodeListe[0].bPInntektListe?.size == 1)
        assertThat(hendelser[0].bidragPeriodeListe[0].bMInntektListe?.size == 1)

        assertThat(hendelser[0].bidragPeriodeListe[1].periodeFra).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(hendelser[0].bidragPeriodeListe[1].periodeTil).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(200))
        assertThat(hendelser[0].bidragPeriodeListe[1].bidragsevne).isEqualTo(BigDecimal.valueOf(3500))
        assertThat(hendelser[0].bidragPeriodeListe[1].underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[1].bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(200))
        assertThat(hendelser[0].bidragPeriodeListe[1].samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe[1].nettoBarnetilleggBP).isEqualTo(BigDecimal.valueOf(1200))
        assertThat(hendelser[0].bidragPeriodeListe[1].nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[1].bPBorMedAndreVoksne).isTrue
        assertThat(hendelser[0].bidragPeriodeListe[1].samværsklasse).isEqualTo(Samværsklasse.DELT_BOSTED)

        assertThat(hendelser[0].bidragPeriodeListe[1].bPInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(1700))
        assertThat(hendelser[0].bidragPeriodeListe[1].bMInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(2500))
        assertThat(hendelser[0].bidragPeriodeListe[1].bPInntektListe?.size == 1)
        assertThat(hendelser[0].bidragPeriodeListe[1].bMInntektListe?.size == 1)
    }

    @Test
    fun `skal behandle hendelse Bidrag uten grunnlag`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidragUtenGrunnlag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"1",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())
        val hendelser = captor.allValues
    }

    @Test
    fun `skal lese vedtakshendelse for aldersjustering fra bidrag-automatisk-jobb uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoAldersjusteringBidrag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"AUTOMATISK",
              "type":"ALDERSJUSTERING",
              "id":"99999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-automatisk-jobb",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(99999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-01T23:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ALDERSJUSTERING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe.first().bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3))
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBP).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().bPBorMedAndreVoksne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
    }

    @Test
    fun `skal lese vedtakshendelse for aldersjustering fra Bisys uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidrag("bisys"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"AUTOMATISK",
              "type":"ALDERSJUSTERING",
              "id":"99999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bisys",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(99999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-02T00:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ALDERSJUSTERING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isTrue
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().underholdskostnad).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().bPsAndelUnderholdskostnad).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsfradrag).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBP).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().bPBorMedAndreVoksne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsklasse).isNull()
    }

    @Test
    fun `skal lese vedtakshendelse Bidrag med netto tilsynsutgift og faktisk utgift uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakmednettotilsynsutgiftogfaktiskutgift.json"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"99999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2025-08-27T11:00:00.000001",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2025-08-27T11:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "12345678901",
                 "kravhaver": "23456789012",
                 "mottaker": "34567890123",
                 "innkreving": "UTEN_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(99999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2025-08-27T11:00:00.000001")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("12345678901")
        assertThat(hendelser[0].kravhaver).isEqualTo("23456789012")
        assertThat(hendelser[0].mottaker).isEqualTo("34567890123")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 1)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 7, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(2170))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(7798.48))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(7925.31))
        assertThat(hendelser[0].bidragPeriodeListe[0].bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3266.75))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(1099))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBP).isEqualTo(BigDecimal.valueOf(613.58))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].bPBorMedAndreVoksne).isFalse
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
        assertThat(hendelser[0].bidragPeriodeListe[0].bPInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(496864))
        assertThat(hendelser[0].bidragPeriodeListe[0].bMInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(708553))
    }

    @Disabled
    @Test
    fun `skal lese vedtakshendelse Bidrag med netto tilsynsutgift og faktisk utgift uten feil2`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/test.json"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"INNKREVING",
              "id":"4796607",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bisys",              
              "vedtakstidspunkt":"2012-10-03T09:44:33.000619",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2012-10-03T09:44:33.000619",    
              "stønadsendringListe": [
                {
                 "type": "OPPFOSTRINGSBIDRAG",
                 "sak": "1210712",
                 "skyldner": "12345678901",
                 "kravhaver": "23456789012",
                 "mottaker": "34567890123",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(4796607)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2025-08-27T11:00:00.000001")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1210712")
        assertThat(hendelser[0].skyldner).isEqualTo("12345678901")
        assertThat(hendelser[0].kravhaver).isEqualTo("23456789012")
        assertThat(hendelser[0].mottaker).isEqualTo("34567890123")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 1)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 7, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(2170))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(7798.48))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(7925.31))
        assertThat(hendelser[0].bidragPeriodeListe[0].bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3266.75))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(1099))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBP).isEqualTo(BigDecimal.valueOf(613.58))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggBM).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].bPBorMedAndreVoksne).isFalse
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
        assertThat(hendelser[0].bidragPeriodeListe[0].bPInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(496864))
        assertThat(hendelser[0].bidragPeriodeListe[0].bMInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(708553))
    }
}
