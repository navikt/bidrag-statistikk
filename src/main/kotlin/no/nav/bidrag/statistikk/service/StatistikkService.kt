package no.nav.bidrag.statistikk.service

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.bo.BidragHendelse
import no.nav.bidrag.statistikk.bo.BidragPeriode
import no.nav.bidrag.statistikk.consumer.BidragVedtakConsumer
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningNettoBarnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSamværsfradrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningVoksneIHustand
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse
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
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class StatistikkService(val hendelserService: HendelserService, val bidragVedtakConsumer: BidragVedtakConsumer) {

    val bidragBehandling = "bidrag-behandling"

    // Behandler mottatt vedtak og sender videre på statistikk-topic
    fun behandleVedtakshendelse(vedtakHendelse: VedtakHendelse) {
        val vedtakDto = hentVedtak(vedtakHendelse.id.toLong())

        LOGGER.info("Henter komplett vedtak for vedtaksid: ${vedtakHendelse.id}")
        SECURE_LOGGER.debug("Henter komplett vedtak for vedtaksid: {} vedtak: {}", vedtakHendelse.id, vedtakDto)

//        behandleVedtakHendelseForskudd(vedtakHendelse, vedtakDto)
        behandleVedtakHendelseBidrag(vedtakHendelse, vedtakDto)
    }

    private fun behandleVedtakHendelseForskudd(vedtakHendelse: VedtakHendelse, vedtakDto: VedtakDto?) {
        vedtakDto?.stønadsendringListe?.filter { it.type == Stønadstype.FORSKUDD && it.beslutning == Beslutningstype.ENDRING }
            ?.forEach { stønadsendring ->
                val forskuddHendelse = ForskuddHendelse(
                    vedtaksid = vedtakHendelse.id.toLong(),
                    vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
                    type = vedtakHendelse.type.name,
                    saksnr = stønadsendring.sak.verdi,
                    kravhaver = stønadsendring.kravhaver.verdi,
                    mottaker = stønadsendring.mottaker.verdi,
                    historiskVedtak = !vedtakDto.kildeapplikasjon.contains(bidragBehandling),
                    forskuddPeriodeListe = stønadsendring.periodeListe.map { periode ->
                        val grunnlagsdata = finnGrunnlagsdataForskudd(vedtakDto.grunnlagListe, periode.grunnlagReferanseListe)

                        if ((
                                grunnlagsdata?.barnetsAldersgruppe == null ||
                                    grunnlagsdata.antallBarnIEgenHusstand == null ||
                                    grunnlagsdata.sivilstand == null ||
                                    grunnlagsdata.barnBorMedBM == null ||
                                    grunnlagsdata.inntektListe?.isEmpty() == true
                                ) &&
                            vedtakDto.kildeapplikasjon.contains(bidragBehandling)
                        ) {
                            SECURE_LOGGER.info(
                                "Fullstendig grunnlag ikke funnet for forskuddsvedtak med vedtaksid: {}, vedtakstype: {}, " +
                                    "resultatkode: {}, beløp: {}",
                                vedtakHendelse.id,
                                vedtakDto.type,
                                periode.resultatkode,
                                periode.beløp,
                            )
                        }
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
                            barnetsAldersgruppe = grunnlagsdata?.barnetsAldersgruppe,
                            antallBarnIEgenHusstand = grunnlagsdata?.antallBarnIEgenHusstand,
                            sivilstand = grunnlagsdata?.sivilstand,
                            barnBorMedBM = grunnlagsdata?.barnBorMedBM,
                            inntektListe = grunnlagsdata?.inntektListe ?: emptyList(),
                        )
                    },
                )
                hendelserService.opprettForskuddshendelse(forskuddHendelse)
            }
    }

    private fun behandleVedtakHendelseBidrag(vedtakHendelse: VedtakHendelse, vedtakDto: VedtakDto?) {
        vedtakDto?.stønadsendringListe?.filter {
            it.type == Stønadstype.BIDRAG ||
                it.type == Stønadstype.BIDRAG18AAR ||
                it.type == Stønadstype.OPPFOSTRINGSBIDRAG &&
                it.beslutning == Beslutningstype.ENDRING
        }
            ?.forEach { stønadsendring ->
                val bidragHendelse = BidragHendelse(
                    vedtaksid = vedtakHendelse.id.toLong(),
                    vedtakstidspunkt = vedtakHendelse.vedtakstidspunkt,
                    type = vedtakHendelse.type.name,
                    saksnr = stønadsendring.sak.verdi,
                    skyldner = stønadsendring.skyldner.verdi,
                    kravhaver = stønadsendring.kravhaver.verdi,
                    mottaker = stønadsendring.mottaker.verdi,
                    historiskVedtak = !vedtakDto.kildeapplikasjon.contains(bidragBehandling),
                    bidragPeriodeListe = stønadsendring.periodeListe.map { periode ->
                        val grunnlagsdata = finnGrunnlagsdataBidrag(vedtakDto.grunnlagListe, periode.grunnlagReferanseListe)

                        // Sjekker på de grunnlagstypene som alltid skal være med og logger hvis noen av de mangler
                        if ((
                                grunnlagsdata?.bidragsevne == null ||
                                    grunnlagsdata.underholdskostnad == null ||
                                    grunnlagsdata.bPsAndelUnderholdskostnad == null ||
                                    grunnlagsdata.bPBorMedAndreVoksne == null ||
                                    grunnlagsdata.deltBosted == null ||
                                    grunnlagsdata.bPInntektListe?.isEmpty() == true ||
                                    grunnlagsdata.bMInntektListe?.isEmpty() == true
                                ) &&
                            vedtakDto.kildeapplikasjon.contains(bidragBehandling)
                        ) {
                            SECURE_LOGGER.info(
                                "Fullstendig grunnlag ikke funnet for bidragsvedtak med vedtaksid: {}, vedtakstype: {}, resultatkode: {}, beløp: {}",
                                vedtakHendelse.id,
                                vedtakDto.type,
                                periode.resultatkode,
                                periode.beløp,
                            )
                        }
                        BidragPeriode(
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
                            bidragsevne = grunnlagsdata?.bidragsevne,
                            underholdskostnad = grunnlagsdata?.underholdskostnad,
                            bPsAndelUnderholdskostnad = grunnlagsdata?.bPsAndelUnderholdskostnad,
                            samværsfradrag = grunnlagsdata?.samværsfradrag,
                            nettoBarnetilleggBP = grunnlagsdata?.nettoBarnetilleggBP,
                            nettoBarnetilleggBM = grunnlagsdata?.nettoBarnetilleggBM,
                            bPBorMedAndreVoksne = grunnlagsdata?.bPBorMedAndreVoksne ?: false,
                            deltBosted = grunnlagsdata?.deltBosted ?: false,
                            bPInntektListe = grunnlagsdata?.bPInntektListe ?: emptyList(),
                            bMInntektListe = grunnlagsdata?.bMInntektListe ?: emptyList(),
                        )
                    },
                )
                hendelserService.opprettBidragshendelse(bidragHendelse)
            }
    }

    fun hentVedtak(vedtaksid: Long): VedtakDto? = bidragVedtakConsumer.hentVedtak(vedtaksid)

    private fun finnGrunnlagsdataForskudd(
        grunnlagListe: List<GrunnlagDto>,
        grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    ): GrunnlagsdataForskudd? {
        // Sjekker først om perioden har grunnlag, hvis ikke returneres null
        if (grunnlagListe.isEmpty()) {
            return null
        }

        // Finn grunnlagsdata
        val respons = GrunnlagsdataForskudd(
            barnetsAldersgruppe = grunnlagListe.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListe),
            antallBarnIEgenHusstand = grunnlagListe.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListe),
            sivilstand = grunnlagListe.finnSivilstandForPeriode(grunnlagsreferanseListe),
            barnBorMedBM = grunnlagListe.finnOmBarnBorMedBMIPeriode(grunnlagsreferanseListe),
            inntektListe = grunnlagListe.finnInntekterForskudd(grunnlagsreferanseListe),
        )

        return respons
    }

    private fun finnGrunnlagsdataBidrag(grunnlagListe: List<GrunnlagDto>, grunnlagsreferanseListe: List<Grunnlagsreferanse>): GrunnlagsdataBidrag? {
        // Sjekker først om perioden har grunnlag, hvis ikke returneres null
        if (grunnlagListe.isEmpty()) {
            return null
        }

        val referanseBP = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
        val referanseBM = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
        val søknadsbarnReferanse = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_SØKNADSBARN)

        // Finn grunnlagsdata
        val respons = GrunnlagsdataBidrag(
            bidragsevne = grunnlagListe.finnBidragevneForPeriode(grunnlagsreferanseListe),
            underholdskostnad = grunnlagListe.finnUnderholdskostnadForPeriode(grunnlagsreferanseListe),
            bPsAndelUnderholdskostnad = grunnlagListe.finnBpsAndelUnderholdskostnadForPeriode(grunnlagsreferanseListe),
            samværsfradrag = grunnlagListe.finnSamværsfradragForPeriode(grunnlagsreferanseListe),
            nettoBarnetilleggBP = grunnlagListe.finnNettoBarnetilleggForPeriode(referanseBP),
            nettoBarnetilleggBM = grunnlagListe.finnNettoBarnetilleggForPeriode(referanseBM),
            bPBorMedAndreVoksne = grunnlagListe.finnBPBorMedAndreVoksneIPeriode(grunnlagsreferanseListe),
            deltBosted = grunnlagListe.finnDeltBostedIPeriode(grunnlagsreferanseListe),
            bPInntektListe = grunnlagListe.finnInntekterBidrag(referanseBP, søknadsbarnReferanse),
            bMInntektListe = grunnlagListe.finnInntekterBidrag(referanseBM, søknadsbarnReferanse),
        )

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

    fun List<GrunnlagDto>.finnInntekterForskudd(grunnlagsreferanseListe: List<Grunnlagsreferanse>): List<Inntekt>? {
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

    fun List<GrunnlagDto>.finnBidragevneForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val bidragevne = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBidragsevne>(
            Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
            sluttberegning,
        ).firstOrNull()
        return bidragevne?.innhold?.beløp
    }

    fun List<GrunnlagDto>.finnUnderholdskostnadForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val underholdskostand = finnOgKonverterGrunnlagSomErReferertAv<DelberegningUnderholdskostnad>(
            Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD,
            sluttberegning,
        ).firstOrNull()
        return underholdskostand?.innhold?.underholdskostnad
    }

    fun List<GrunnlagDto>.finnBpsAndelUnderholdskostnadForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val underholdskostand = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBidragspliktigesAndel>(
            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
            sluttberegning,
        ).firstOrNull()
        return underholdskostand?.innhold?.andelBeløp
    }

    fun List<GrunnlagDto>.finnSamværsfradragForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val underholdskostand = finnOgKonverterGrunnlagSomErReferertAv<DelberegningSamværsfradrag>(
            Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
            sluttberegning,
        ).firstOrNull()
        return underholdskostand?.innhold?.beløp
    }

    fun List<GrunnlagDto>.finnNettoBarnetilleggForPeriode(referanseTilRolle: String): BigDecimal? {
        val underholdskostand = filtrerOgKonverterBasertPåFremmedReferanse<DelberegningNettoBarnetillegg>(
            Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG,
            referanseTilRolle,
        ).firstOrNull()
        return underholdskostand?.innhold?.summertNettoBarnetillegg
    }

    fun List<GrunnlagDto>.finnBPBorMedAndreVoksneIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val bostatusPeriode = finnOgKonverterGrunnlagSomErReferertAv<DelberegningVoksneIHustand>(
            Grunnlagstype.DELBEREGNING_VOKSNE_I_HUSSTAND,
            sluttberegning,
        ).firstOrNull()
        return bostatusPeriode?.innhold?.borMedAndreVoksne
    }

    fun List<GrunnlagDto>.finnDeltBostedIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val samværsperiode = finnOgKonverterGrunnlagSomErReferertAv<SamværsperiodeGrunnlag>(
            Grunnlagstype.SAMVÆRSPERIODE,
            sluttberegning,
        ).firstOrNull()
        return samværsperiode?.innhold?.samværsklasse == Samværsklasse.DELT_BOSTED
    }

    fun List<GrunnlagDto>.finnInntekterBidrag(referanseTilRolle: String, søknadsbarnReferanse: String): List<Inntekt>? {
        val inntekter = filtrerOgKonverterBasertPåFremmedReferanse<InntektsrapporteringPeriode>(
            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            referanseTilRolle,
        ).filter { it.innhold.valgt }
            .filter { it.innhold.gjelderBarn == null || it.innhold.gjelderBarn == søknadsbarnReferanse }
        return inntekter.map { inntekt ->
            Inntekt(
                type = inntekt.innhold.inntektsrapportering.name,
                beløp = inntekt.innhold.beløp,
            )
        }
    }

    fun finnReferanseTilRolle(grunnlagListe: List<GrunnlagDto>, grunnlagstype: Grunnlagstype) = grunnlagListe
        .firstOrNull { it.type == grunnlagstype }?.referanse ?: throw NoSuchElementException("Grunnlagstype $grunnlagstype mangler i input")

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

data class GrunnlagsdataBidrag(
    val bidragsevne: BigDecimal?,
    val underholdskostnad: BigDecimal?,
    val bPsAndelUnderholdskostnad: BigDecimal?,
    val samværsfradrag: BigDecimal?,
    val nettoBarnetilleggBP: BigDecimal?,
    val nettoBarnetilleggBM: BigDecimal?,
    val bPBorMedAndreVoksne: Boolean?,
    val deltBosted: Boolean?,
    val bPInntektListe: List<Inntekt>?,
    val bMInntektListe: List<Inntekt>?,
)
