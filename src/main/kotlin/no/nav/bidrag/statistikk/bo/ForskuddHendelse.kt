package no.nav.bidrag.statistikk.bo

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ForskuddHendelse(
    val vedtaksid: Long,
    val vedtakstidspunkt: LocalDateTime,
    val type: String,
    val kravhaver: String,
    val mottaker: String,
    val forskuddPeriodeListe: List<ForskuddPeriode>,
)

data class ForskuddPeriode(
    val periodeFra: LocalDate,
    val periodeTil: LocalDate?,
    val beløp: BigDecimal?,
    val resultat: String,
    val barnetsAldersgruppe: String,
    val antallBarnIEgenHusstand: Double,
    val sivilstand: String,
    val barnBorMedBM: Boolean,
    val inntektListe: List<Inntekt>,
)

data class Inntekt(
    val type: String,
    val beløp: BigDecimal,
)

