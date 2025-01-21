package no.nav.bidrag.statistikk.hendelse

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.BidragStatistikkTest.Companion.TEST_PROFILE
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDto
import no.nav.bidrag.statistikk.TestUtil.Companion.stubHenteVedtak
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
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

    @BeforeEach
    fun init() {
        every { kafkaTemplate.send(any<ProducerRecord<String, String>>()) } returns CompletableFuture.completedFuture(SendResult(null, null))
        every { kafkaTemplate.afterSingletonsInstantiated() } returns Unit
    }

    @Test
    fun `skal lese vedtakshendelse uten feil`() {
        stubHenteVedtak(byggVedtakDto())
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
              "stønadsendringListe": [
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
    }

    @Test
    fun `skal behandle forskudd`() {
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
              }        
              ],
              "sporingsdata":
                {
                "correlationId":"korrelasjon_id-123213123213"            
                }
            }
            """.trimIndent(),
        )
    }
}
