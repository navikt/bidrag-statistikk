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
import no.nav.bidrag.statistikk.TestUtil.Companion.stubHenteVedtak
import no.nav.bidrag.statistikk.bo.BidragHendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
              "id":"9921731",
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
    fun `skal lese vedtakshendelse Forskudd uten grunnlag uten feil`() {
        stubHenteVedtak(byggVedtakDtoUtenGrunnlag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"9921731",
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
        stubHenteVedtak(byggVedtakDtoBidrag())
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
        assertThat(hendelser[0].vedtaksid).isEqualTo(1)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-01T23:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isEqualTo(BigDecimal.valueOf(3500))
        assertThat(hendelser[0].bidragPeriodeListe.first().underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe.first().bPsAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(200))
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBP).isEqualTo(BigDecimal.valueOf(100))
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggBM).isEqualTo(BigDecimal.valueOf(10))
        assertThat(hendelser[0].bidragPeriodeListe.first().bPBorMedAndreVoksne).isTrue
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsklasse).isEqualTo(Samværsklasse.DELT_BOSTED)
    }

    @Test
    fun `skal ikke behandle hendelse Bidrag hvis vedtak ikke inneholder grunnlag`() {
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
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isNull()
    }

    @Test
    fun `skal lese vedtakshendelse for aldersjustering uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoAldersjusteringBidrag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"AUTOMATISK",
              "type":"ALDERSJUSTERING",
              "id":"1",
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
        assertThat(hendelser[0].vedtaksid).isEqualTo(1)
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
}
