package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.consumer.BidragVedtakConsumer
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnOgKonverterGrunnlagSomErReferertAv
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import no.nav.bidrag.transport.behandling.statistikk.ForskuddPeriode
import no.nav.bidrag.transport.behandling.statistikk.Inntekt
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class StatistikkService(val hendelserService: HendelserService, val bidragVedtakConsumer: BidragVedtakConsumer) {

    // Behandler mottatt vedtak og sender videre på statistikk-topic
    fun behandleVedtakshendelse(vedtakHendelse: VedtakHendelse) {
        val vedtak = hentVedtak(vedtakHendelse.id.toLong())

        SECURE_LOGGER.info("Vedtak hentet for vedtakshendelse med id ${vedtakHendelse.id} vedtak: $vedtak")

        vedtak?.stønadsendringListe?.forEach { stønadsendring ->
            val forskuddHendelse = ForskuddHendelse(
                vedtaksid = vedtakHendelse.id.toLong(),
                vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
                type = vedtakHendelse.type.name,
                saksnr = stønadsendring.sak.verdi,
                kravhaver = stønadsendring.kravhaver.verdi,
                mottaker = stønadsendring.mottaker.verdi,
                forskuddPeriodeListe = stønadsendring.periodeListe.map { periode ->
                    val grunnlagsdata = finnGrunnlagsdata(vedtak.grunnlagListe.toList(), periode.grunnlagReferanseListe)
                    ForskuddPeriode(
                        periodeFra = LocalDate.of(periode.periode.fom.year, periode.periode.fom.month, 1),
                        periodeTil = if (periode.periode.til == null) {
                            null
                        } else {
                            LocalDate.of(
                                periode.periode.til!!.year,
                                periode.periode.til!!.month,
                                1,
                            )
                        },
                        beløp = periode.beløp,
                        resultat = periode.resultatkode,
                        barnetsAldersgruppe = grunnlagsdata.barnetsAldersgruppe!!,
                        antallBarnIEgenHusstand = grunnlagsdata.antallBarnIEgenHusstand!!,
                        sivilstand = grunnlagsdata.sivilstand!!,
                        barnBorMedBM = grunnlagsdata.barnBorMedBM!!,
                        inntektListe = grunnlagsdata.inntektListe!!,
                    )
                },
            )
            hendelserService.opprettHendelse(forskuddHendelse)
        }
    }

    fun hentVedtak(vedtaksid: Long): VedtakDto? = bidragVedtakConsumer.hentVedtak(vedtaksid)

    private fun finnGrunnlagsdata(grunnlagListe: List<GrunnlagDto>, grunnlagsreferanseListe: List<Grunnlagsreferanse>): GrunnlagsdataForskudd {
        // Finn grunnlagsdata
        val respons = GrunnlagsdataForskudd(
            barnetsAldersgruppe = grunnlagListe.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListe),
            antallBarnIEgenHusstand = grunnlagListe.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListe),
            sivilstand = grunnlagListe.finnSivilstandForPeriode(grunnlagsreferanseListe),
            barnBorMedBM = grunnlagListe.finnOmBarnBorMedBMIPeriode(grunnlagsreferanseListe),
            inntektListe = grunnlagListe.finnInntekter(grunnlagsreferanseListe),
        )
        if (respons.barnetsAldersgruppe == null ||
            respons.antallBarnIEgenHusstand == null ||
            respons.sivilstand == null ||
            respons.barnBorMedBM == null ||
            respons.inntektListe == null
        ) {
            throw Exception("Klarte ikke å hente grunnlagsdata for forskuddsvedtak, $respons")
        }

        return respons
    }

    fun List<GrunnlagDto>.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        return sluttberegning.innholdTilObjekt<SluttberegningForskudd>().aldersgruppe.name
    }

    fun List<GrunnlagDto>.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Double {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return 0.0
        val antallBarnIEgenHusstandPeriode = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBarnIHusstand>(
            Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
            sluttberegning,
        ).firstOrNull()
        return antallBarnIEgenHusstandPeriode?.innhold?.antallBarn ?: 0.0
    }

    fun List<GrunnlagDto>.finnSivilstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val sivilstandPeriode = finnOgKonverterGrunnlagSomErReferertAv<SivilstandPeriode>(
            Grunnlagstype.SIVILSTAND_PERIODE,
            sluttberegning,
        ).firstOrNull()
        return sivilstandPeriode?.innhold?.sivilstand?.name
    }

    fun List<GrunnlagDto>.finnOmBarnBorMedBMIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val bostatusPeriode = finnOgKonverterGrunnlagSomErReferertAv<BostatusPeriode>(
            Grunnlagstype.BOSTATUS_PERIODE,
            sluttberegning,
        ).firstOrNull()
        return bostatusPeriode?.innhold?.bostatus == Bostatuskode.MED_FORELDER
    }

    fun List<GrunnlagDto>.finnInntekter(grunnlagsreferanseListe: List<Grunnlagsreferanse>): List<Inntekt>? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val inntekter = finnOgKonverterGrunnlagSomErReferertAv<InntektsrapporteringPeriode>(
            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            sluttberegning,
        )
        return inntekter.map { inntekt ->
            Inntekt(
                type = inntekt.innhold.inntektsrapportering.name,
                beløp = inntekt.innhold.beløp,
            )
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatistikkService::class.java)
    }
}

data class GrunnlagsdataForskudd(
    val barnetsAldersgruppe: String?,
    val antallBarnIEgenHusstand: Double?,
    val sivilstand: String?,
    val barnBorMedBM: Boolean?,
    val inntektListe: List<Inntekt>?,
)
