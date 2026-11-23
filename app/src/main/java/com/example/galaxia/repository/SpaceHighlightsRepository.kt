package com.example.galaxia.repository

import android.util.Log
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.model.CloseApproachData
import com.example.galaxia.data.model.DonkiNotification
import com.example.galaxia.data.model.NearEarthObject
import com.example.galaxia.data.model.SpaceHighlight
import com.example.galaxia.data.remote.NasaApiService
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.translation.MlKitTranslationService
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Repositório das notícias (DONKI – clima espacial) e curiosidades (NeoWs – asteroides
 * próximos) exibidas no feed junto com as fotos do APOD.
 *
 * As duas fontes são complementares: se uma falhar, o feed segue com a outra (ou só
 * com as fotos). O resultado fica em cache por [CACHE_DURATION_MS] para poupar o
 * limite de requisições da chave da NASA.
 */
class SpaceHighlightsRepository(
    private val apiService: NasaApiService = RetrofitClient.nasaApiService,
    private val translationService: TranslationService = MlKitTranslationService(),
    private val today: () -> LocalDate = { LocalDate.now() }
) : BaseRepository {

    private val mutex = Mutex()
    private var cached: List<SpaceHighlight>? = null
    private var cachedAt = 0L

    /** Notícias e curiosidades, intercaladas. Nunca lança erro de rede: devolve lista vazia. */
    suspend fun getHighlights(forceRefresh: Boolean = false): List<SpaceHighlight> = mutex.withLock {
        val now = System.currentTimeMillis()
        cached?.let { if (!forceRefresh && now - cachedAt < CACHE_DURATION_MS) return it }

        val result = coroutineScope {
            val news = async { safeFetch("DONKI") { fetchSpaceWeatherNews() } }
            val curiosities = async { safeFetch("NeoWs") { fetchAsteroidCuriosities() } }
            interleave(news.await(), curiosities.await())
        }

        // Lista vazia (as duas fontes falharam) não vai para o cache, para tentar de novo depois
        if (result.isNotEmpty()) {
            cached = result
            cachedAt = now
        }
        result
    }

    private suspend fun safeFetch(
        source: String,
        block: suspend () -> List<SpaceHighlight>
    ): List<SpaceHighlight> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Falha ao buscar $source", e)
        emptyList()
    }

    private fun interleave(a: List<SpaceHighlight>, b: List<SpaceHighlight>): List<SpaceHighlight> {
        val result = ArrayList<SpaceHighlight>(a.size + b.size)
        for (i in 0 until maxOf(a.size, b.size)) {
            a.getOrNull(i)?.let { result += it }
            b.getOrNull(i)?.let { result += it }
        }
        return result
    }

    // --- DONKI: notícias de clima espacial ---

    private suspend fun fetchSpaceWeatherNews(): List<SpaceHighlight> = coroutineScope {
        val end = today()
        val start = end.minusDays(DONKI_DAYS_BACK)
        val notifications = apiService.getDonkiNotifications(
            startDate = start.format(ISO_DATE),
            endDate = end.format(ISO_DATE)
        )

        notifications
            // O resumo semanal ("Report") é longo e cheio de tabelas: não funciona como notícia
            .filter { !it.messageID.isNullOrBlank() && it.messageType != "Report" }
            .sortedByDescending { it.messageIssueTime.orEmpty() }
            .take(MAX_NEWS)
            .map { notification -> async { notification.toHighlight() } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun DonkiNotification.toHighlight(): SpaceHighlight? {
        val summary = extractSummary(messageBody.orEmpty())
        if (summary.isBlank()) return null

        return SpaceHighlight(
            id = "donki_$messageID",
            type = FavoriteType.NEWS,
            title = donkiTitle(messageType, summary),
            date = messageIssueTime.orEmpty().take(10),
            body = translationService.translateText(summary),
            source = "NASA DONKI",
            sourceUrl = messageURL
        )
    }

    /**
     * Mantém só a seção "## Summary:" da mensagem, sem os links das animações
     * nem os identificadores internos, que não interessam a quem lê o feed.
     */
    private fun extractSummary(body: String): String {
        val start = body.indexOf("## Summary:")
        if (start < 0) return ""
        val end = body.indexOf("## Notes:", start).takeIf { it > 0 } ?: body.length

        return body.substring(start + "## Summary:".length, end)
            .lines()
            .map { it.trim() }
            .filterNot { line ->
                line.startsWith("http", ignoreCase = true) ||
                    line.startsWith("Activity ID", ignoreCase = true) ||
                    line.startsWith("Links to the movies", ignoreCase = true) ||
                    line.startsWith("##") ||
                    LIST_MARKER_ONLY.matches(line)
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun donkiTitle(type: String?, summary: String): String {
        val isUpdate = summary.startsWith("Update", ignoreCase = true)
        val isMultiple = summary.startsWith("Multiple", ignoreCase = true)

        val base = when (type) {
            "CME" -> when {
                isMultiple -> "Múltiplas ejeções de massa coronal detectadas no Sol"
                else -> {
                    val speed = CME_SPEED.find(summary)?.groupValues?.get(1)
                    if (speed != null) "Ejeção de massa coronal a ~$speed km/s"
                    else "Ejeção de massa coronal detectada no Sol"
                }
            }
            "FLR" -> FLARE_CLASS.find(summary)?.groupValues?.get(1)
                ?.let { "Erupção solar de classe $it detectada" }
                ?: "Erupção solar detectada"
            "GST" -> "Tempestade geomagnética em andamento"
            "SEP" -> "Aumento de partículas solares energéticas"
            "IPS" -> "Choque interplanetário detectado"
            "MPC" -> "Vento solar comprime a magnetosfera da Terra"
            "RBE" -> "Elétrons energéticos elevados no cinturão de radiação"
            else -> "Alerta de clima espacial"
        }
        return if (isUpdate) "Atualização: $base" else base
    }

    // --- NeoWs: curiosidades sobre asteroides ---

    private suspend fun fetchAsteroidCuriosities(): List<SpaceHighlight> {
        val date = today().format(ISO_DATE)
        val response = apiService.getNeoFeed(startDate = date, endDate = date)

        val asteroids = response.nearEarthObjects?.get(date).orEmpty()
            .mapNotNull { neo -> neo.approachOn(date)?.let { neo to it } }
            .filter { (neo, approach) -> neo.id != null && approach.missKm() != null }
        if (asteroids.isEmpty()) return emptyList()

        val total = response.elementCount ?: asteroids.size
        val closest = asteroids.minBy { (_, approach) -> approach.missKm()!! }
        val largest = asteroids.maxBy { (neo, _) -> neo.maxDiameter() ?: 0.0 }

        return listOf(closest to true, largest to false)
            .distinctBy { (pair, _) -> pair.first.id }
            .take(MAX_CURIOSITIES)
            .map { (pair, isClosest) -> buildAsteroidHighlight(pair.first, pair.second, date, total, isClosest) }
    }

    private fun NearEarthObject.approachOn(date: String): CloseApproachData? =
        closeApproachData?.firstOrNull { it.date == date && (it.orbitingBody ?: "Earth") == "Earth" }

    private fun CloseApproachData.missKm(): Double? = missDistance?.kilometers?.toDoubleOrNull()

    private fun NearEarthObject.maxDiameter(): Double? = estimatedDiameter?.meters?.max

    private fun buildAsteroidHighlight(
        neo: NearEarthObject,
        approach: CloseApproachData,
        date: String,
        totalToday: Int,
        isClosest: Boolean
    ): SpaceHighlight {
        val name = neo.displayName
        val km = approach.missKm() ?: 0.0
        val lunar = approach.missDistance?.lunar?.toDoubleOrNull()
        val kmh = approach.relativeVelocity?.kilometersPerHour?.toDoubleOrNull()
        val kms = approach.relativeVelocity?.kilometersPerSecond?.toDoubleOrNull()
        val minM = neo.estimatedDiameter?.meters?.min
        val maxM = neo.maxDiameter()
        val hazardous = neo.isPotentiallyHazardous == true
        val time = approach.dateFull?.substringAfter(' ', "")?.takeIf { it.isNotBlank() }

        val title = if (isClosest) {
            "Asteroide $name passa hoje a ${formatDistance(km)} da Terra"
        } else {
            val size = maxM?.let { " de até ${formatInt(it)} m" }.orEmpty()
            "Asteroide$size, o $name, cruza a vizinhança da Terra hoje"
        }

        val body = buildString {
            append("O asteroide $name faz hoje")
            time?.let { append(", às $it (UTC),") }
            append(" sua maior aproximação da Terra, passando a cerca de ${formatDistance(km)}")
            lunar?.let {
                if (it < 1.0) append(" — mais perto do que a Lua!")
                else append(" — o equivalente a ${formatDecimal(it)} vezes a distância entre a Terra e a Lua.")
            } ?: append(".")
            append("\n\n")

            if (minM != null && maxM != null) {
                append("Diâmetro estimado: entre ${formatInt(minM)} e ${formatInt(maxM)} metros.\n")
            }
            if (kmh != null) {
                append("Velocidade relativa: ${formatInt(kmh)} km/h")
                kms?.let { append(" (${formatDecimal(it)} km/s)") }
                append(".\n")
            }
            append(
                if (hazardous) "Classificação: potencialmente perigoso."
                else "Classificação: não é considerado potencialmente perigoso."
            )
            append("\n\n")

            if (isClosest) {
                append("Ao todo, $totalToday ")
                append(if (totalToday == 1) "asteroide passa" else "asteroides passam")
                append(" perto da Terra hoje, segundo o NeoWs, o serviço da NASA que acompanha objetos próximos.")
            } else {
                append(
                    "Você sabia? A NASA chama de \"potencialmente perigosos\" os asteroides com mais de " +
                        "cerca de 140 metros cuja órbita chega a menos de 7,5 milhões de km da órbita da Terra. " +
                        "Isso não indica risco de colisão: significa apenas que eles merecem acompanhamento constante."
                )
            }
        }

        return SpaceHighlight(
            id = "neo_${neo.id}_$date",
            type = FavoriteType.CURIOSITY,
            title = title,
            date = date,
            body = body,
            source = "NASA NeoWs",
            sourceUrl = neo.nasaJplUrl,
            badge = if (hazardous) "Potencialmente perigoso" else null
        )
    }

    // --- Formatação em pt-BR ---

    private fun formatDistance(km: Double): String {
        val millions = km / 1_000_000
        return when {
            millions >= 2 -> "${formatDecimal(millions)} milhões de km"
            millions >= 1 -> "${formatDecimal(millions)} milhão de km"
            else -> "${formatInt(km)} km"
        }
    }

    private fun formatInt(value: Double): String =
        NumberFormat.getIntegerInstance(PT_BR).format(Math.round(value))

    private fun formatDecimal(value: Double): String =
        NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(value)

    companion object {
        private const val TAG = "SpaceHighlightsRepo"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L
        private const val DONKI_DAYS_BACK = 7L
        private const val MAX_NEWS = 4
        private const val MAX_CURIOSITIES = 2

        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
        private val CME_SPEED = Regex("""Estimated speed:\s*~?\s*(\d+)\s*km/s""", RegexOption.IGNORE_CASE)
        private val FLARE_CLASS = Regex("""\b([ABCMX]\d+(?:\.\d+)?)\b""")
        private val LIST_MARKER_ONLY = Regex("""^\(?[a-z0-9]\)?[.:]?$""", RegexOption.IGNORE_CASE)
    }
}
