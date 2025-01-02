package no.nav.bidrag.statistikk

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
import no.nav.bidrag.transport.behandling.vedtak.Behandlingsreferanse
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class TestUtil {

    companion object {
        fun byggVedtakHendelse(): VedtakHendelse {
            return VedtakHendelse(
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
        }

        fun byggVedtakDto(): VedtakDto {
            return VedtakDto(
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
                                periode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 7)),
                                beløp = BigDecimal.valueOf(1),
                                valutakode = "NOK",
                                resultatkode = "A",
                                delytelseId = "delytelseId1",
                                grunnlagReferanseListe = emptyList(),
                            ),
                            VedtakPeriodeDto(
                                periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                                beløp = BigDecimal.valueOf(2),
                                valutakode = "NOK",
                                resultatkode = "A",
                                delytelseId = "delytelseId1",
                                grunnlagReferanseListe = emptyList(),
                            ),
                        ),
                    ),
                ),
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
            )
        }
    }
}
