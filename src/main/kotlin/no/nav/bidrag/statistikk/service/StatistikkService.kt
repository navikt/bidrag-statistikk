package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.statistikk.consumer.BidragVedtakConsumer
import no.nav.bidrag.statistikk.bo.ForskuddHendelse
import no.nav.bidrag.statistikk.bo.ForskuddPeriode
import no.nav.bidrag.statistikk.bo.Inntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjektListe
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
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

        vedtak?.stønadsendringListe?.forEach { stønadsendring ->
            val forskuddHendelse = ForskuddHendelse(
                vedtaksid = vedtakHendelse.id.toLong(),
                vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
                type = vedtakHendelse.type.name,
                kravhaver = stønadsendring.kravhaver.verdi,
                mottaker = stønadsendring.mottaker.verdi,
                forskuddPeriodeListe = stønadsendring.periodeListe.map { periode ->
                    val grunnlagsdata = finnGrunnlagsdata(vedtak.grunnlagListe.toList(), periode.grunnlagReferanseListe)
                    ForskuddPeriode(
                        periodeFra = LocalDate.of(periode.periode.fom.year, periode.periode.fom.month, 1),
                        periodeTil = if (periode.periode.til == null) null else LocalDate.of(periode.periode.til!!.year, periode.periode.til!!.month, 1),
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


    fun hentVedtak(vedtaksid: Long): VedtakDto? {
        return bidragVedtakConsumer.hentVedtak(vedtaksid)
    }

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
            throw Exception("Klarte ikke å hente grunnlagsdata for forskuddsvedtak")
        }

        return respons
    }


    fun List<GrunnlagDto>.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        return sluttberegning.innholdTilObjekt<SluttberegningForskudd>().aldersgruppe.name
    }

    fun List<GrunnlagDto>.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Double? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val antallBarnIEgenHusstandPeriode =
            find {
                it.type == Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND &&
                    sluttberegning.grunnlagsreferanseListe.contains(
                        it.referanse,
                    )
            }
        return antallBarnIEgenHusstandPeriode?.innholdTilObjekt<DelberegningBarnIHusstand>()?.antallBarn
    }

    fun List<GrunnlagDto>.finnSivilstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val sivilstandPeriode =
            find {
                it.type == Grunnlagstype.SIVILSTAND_PERIODE &&
                    sluttberegning.grunnlagsreferanseListe.contains(
                        it.referanse,
                    )
            }
        return sivilstandPeriode?.innholdTilObjekt<SivilstandPeriode>()?.sivilstand?.name
    }

    fun List<GrunnlagDto>.finnOmBarnBorMedBMIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val bostatusPeriode =
            find {
                it.type == Grunnlagstype.BOSTATUS_PERIODE &&
                    sluttberegning.grunnlagsreferanseListe.contains(
                        it.referanse,
                    )
            }
        return bostatusPeriode?.innholdTilObjekt<BostatusPeriode>()?.bostatus == Bostatuskode.MED_FORELDER
    }

    fun List<GrunnlagDto>.finnInntekter(grunnlagsreferanseListe: List<Grunnlagsreferanse>): List<Inntekt>? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val inntekter =
            filter {
                it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE &&
                    sluttberegning.grunnlagsreferanseListe.contains(
                        it.referanse,
                    )
            }
        return inntekter.innholdTilObjekt<InntektsrapporteringPeriode>()?.map { inntekt ->
            Inntekt(
                type = inntekt.inntektsrapportering.name,
                beløp = inntekt.beløp,
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