package no.nav.bidrag.statistikk

import com.fasterxml.jackson.databind.node.POJONode
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.AldersgruppeForskudd
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.vedtak.Behandlingsreferanse
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class TestUtil {

    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder = aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        fun stubHenteVedtak(responseObjekt: VedtakDto? = null, navnResponsfil: String = "vedtak_response.json", status: HttpStatus = HttpStatus.OK) {
            val response =
                if (responseObjekt != null) {
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(
                            commonObjectmapper.writeValueAsString(
                                responseObjekt,
                            ),
                        )
                } else {
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBodyFile(navnResponsfil)
                }
            WireMock.stubFor(
                WireMock.get(urlMatching("/vedtak/vedtak(.*)")).willReturn(
                    response,
                ),
            )
        }

        fun byggVedtakHendelse(): VedtakHendelse = VedtakHendelse(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            id = 1,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            stønadsendringListe = listOf(
                Stønadsendring(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    listOf(
                        Periode(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 7)),
                            beløp = BigDecimal.valueOf(1),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                        ),
                        Periode(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = listOf(
                Behandlingsreferanse(
                    kilde = BehandlingsrefKilde.BISYS_SØKNAD.toString(),
                    referanse = "referanse1",
                ),
            ),
            sporingsdata = Sporingsdata("test"),
        )

        fun byggVedtakDto(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = listOf(
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                    type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                    innhold = POJONode(
                        Person(
                            ident = Personident("16498311338"),
                            fødselsdato = LocalDate.parse("1983-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_SØKNADSBARN_20180718_826",
                    type = Grunnlagstype.PERSON_SØKNADSBARN,
                    innhold = POJONode(
                        Person(
                            ident = Personident("12345678901"),
                            navn = "Ola Nordmann",
                            fødselsdato = LocalDate.parse("2018-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                    type = Grunnlagstype.SLUTTBEREGNING_FORSKUDD,
                    innhold = POJONode(
                        SluttberegningForskudd(
                            beløp = BigDecimal.ONE,
                            resultatKode = Resultatkode.ORDINÆRT_FORSKUDD_75_PROSENT,
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            aldersgruppe = AldersgruppeForskudd.ALDER_0_10_ÅR,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                        "delberegning_DELBEREGNING_BARN_I_HUSSTAND_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202402",
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202401",
                        "person_PERSON_SØKNADSBARN_20180718_826",
                        "sivilstand_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "sivilstand_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101",
                    type = Grunnlagstype.SIVILSTAND_PERIODE,
                    innhold = POJONode(
                        SivilstandPeriode(
                            manueltRegistrert = true,
                            sivilstand = Sivilstandskode.BOR_ALENE_MED_BARN,
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101_2868",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                            beløp = BigDecimal.valueOf(1000),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    innhold = POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            totalinntekt = BigDecimal.valueOf(1000),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101_2868",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_BARN_I_HUSSTAND_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202402",
                    type = Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
                    innhold = POJONode(
                        DelberegningBarnIHusstand(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            antallBarn = 1.0,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "bostatus_person_PERSON_HUSSTANDSMEDLEM_20151210_964_20240201",
                        "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    ),
                ),
                GrunnlagDto(
                    referanse = "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    type = Grunnlagstype.BOSTATUS_PERIODE,
                    innhold = POJONode(
                        BostatusPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            bostatus = Bostatuskode.MED_FORELDER,
                            relatertTilPart = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                            manueltRegistrert = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "innhentet_husstandsmedlem_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
            ),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2024, 8)),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 8), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoUtenGrunnlag(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = emptyList(),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoUtenForskudd(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = emptyList(),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )
    }
}
