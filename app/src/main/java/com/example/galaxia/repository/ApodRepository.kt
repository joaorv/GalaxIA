package com.example.galaxia.repository

import androidx.core.text.HtmlCompat
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.model.ApodSearchPage
import com.example.galaxia.data.model.SearchFilter
import com.example.galaxia.data.model.SiteSearchResult
import com.example.galaxia.data.model.matchesFilter
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.ApodMockData
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.translation.AppTranslation
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import retrofit2.HttpException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Repositório responsável pela comunicação com a API NASA APOD e tradução automática dos conteúdos.
 */
class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val translationService: TranslationService = AppTranslation.service
) : BaseRepository {

    // Cache em memória para otimizar e evitar re-traduções do mesmo item durante a execução
    private val translationCache = ConcurrentHashMap<String, Pair<String, String>>()

    // Total de APODs publicados, usado para sortear páginas no modo aleatório
    @Volatile
    private var totalApods: Int? = null

    // --- Remoto (NASA APOD + Tradução Automática + Fallback de Contingência) ---

    /** Últimas fotos já traduzidas. */
    suspend fun getApodList(count: Int = 10): List<ApodResponse> = coroutineScope {
        fetchLatestApods(count).map { async { translateApod(it) } }.awaitAll()
    }

    /**
     * Últimas fotos como vêm da API, sem traduzir. A tradução no aparelho é a parte lenta
     * do carregamento, então o feed busca primeiro e traduz depois, na ordem em que os
     * itens aparecem na tela (veja [translateApod] e [translateTitle]).
     */
    suspend fun fetchLatestApods(
        count: Int = 10,
        allowMockFallback: Boolean = true
    ): List<ApodResponse> {
        return try {
            apiService.getApodList(perPage = count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!allowMockFallback) throw e
            // Em caso de limitação da API da NASA ou erro de conexão,
            // retorna dados de contingência para que o app continue funcionando normalmente.
            ApodMockData.sampleApods
        }
    }

    /**
     * Sorteia [count] APODs de todo o acervo (desde 1995).
     * Como a API não oferece ordenação aleatória, cada item é buscado em uma página
     * sorteada com 'per_page=1', usando o total informado no cabeçalho X-WP-Total.
     */
    suspend fun getRandomApods(count: Int = 10): List<ApodResponse> = coroutineScope {
        fetchRandomApods(count).map { async { translateApod(it) } }.awaitAll()
    }

    /** Mesmo sorteio de [getRandomApods], sem traduzir. */
    suspend fun fetchRandomApods(
        count: Int = 10,
        allowMockFallback: Boolean = true
    ): List<ApodResponse> = coroutineScope {
        try {
            val total = totalApods ?: fetchTotalApods().also { totalApods = it }
            val pages = generateSequence { Random.nextInt(1, total + 1) }
                .distinct()
                .take(minOf(count, total))
                .toList()

            val items = pages.map { page ->
                async {
                    // Uma página que falhar não derruba as demais
                    try {
                        apiService.getApodList(perPage = 1, page = page).firstOrNull()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll()
                .filterNotNull()

            if (items.isNotEmpty()) {
                items
            } else if (allowMockFallback) {
                ApodMockData.sampleApods.shuffled()
            } else {
                throw IOException("Não foi possível atualizar as fotos no momento.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (allowMockFallback) ApodMockData.sampleApods.shuffled() else throw e
        }
    }

    private suspend fun fetchTotalApods(): Int {
        val response = apiService.getApodListResponse()
        return response.headers()["X-WP-Total"]?.toIntOrNull()
            ?: throw IllegalStateException("Cabeçalho X-WP-Total ausente (HTTP ${response.code()})")
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        val dateCode = formatDateToCode(date)
        val original = apiService.getApodByDate(dateCode = dateCode)
        return translateApod(original)
    }

    // --- Pesquisa ---

    /**
     * Pesquisa APODs em todo o acervo do science.nasa.gov.
     *
     * Na primeira página, a consulta (normalmente em português) é traduzida para inglês,
     * idioma do conteúdo da NASA. Se o termo traduzido não trouxer nada, tenta o texto
     * original (útil para nomes próprios como "Hale-Bopp"). Nas páginas seguintes, passe
     * o [ApodSearchPage.searchTerm] devolvido para manter o mesmo termo.
     *
     * Diferente do feed, aqui erros de rede são propagados: dados de contingência
     * não fazem sentido como resultado de pesquisa.
     */
    suspend fun searchApods(
        query: String,
        page: Int = 1,
        searchTerm: String? = null,
        filter: SearchFilter = SearchFilter.ALL,
        perPage: Int = SEARCH_PAGE_SIZE
    ): ApodSearchPage {
        val term = searchTerm ?: translationService.translateText(query, "pt", "en").trim()
        var result = fetchFilteredSearchPage(term, page, filter, perPage)

        val original = query.trim()
        if (searchTerm == null && result.items.isEmpty() && !term.equals(original, ignoreCase = true)) {
            result = fetchFilteredSearchPage(original, page, filter, perPage)
        }
        return result
    }

    private suspend fun fetchFilteredSearchPage(
        term: String,
        startPage: Int,
        filter: SearchFilter,
        targetCount: Int
    ): ApodSearchPage = coroutineScope {
        var currentPage = startPage
        var totalPages = startPage
        var totalResults = 0
        val collectedItems = mutableListOf<ApodResponse>()
        var pagesFetched = 0
        val maxPagesPerFetch = if (filter == SearchFilter.ALL) 1 else 5

        while (collectedItems.size < targetCount && currentPage <= totalPages && pagesFetched < maxPagesPerFetch) {
            val response = apiService.searchSite(term = term, page = currentPage, perPage = SEARCH_PAGE_SIZE)
            if (!response.isSuccessful) {
                if (collectedItems.isNotEmpty()) break
                throw HttpException(response)
            }

            totalPages = response.headers()["X-WP-TotalPages"]?.toIntOrNull() ?: currentPage
            totalResults = response.headers()["X-WP-Total"]?.toIntOrNull() ?: totalResults

            val rawItems = response.body().orEmpty()
                .mapNotNull { parseSearchResult(it) }
                .distinctBy { it.date }
                .map { stub -> async { loadSearchResult(stub) } }
                .awaitAll()

            val filtered = rawItems.filter { it.matchesFilter(filter) }
            for (item in filtered) {
                if (collectedItems.none { it.date == item.date }) {
                    collectedItems.add(item)
                }
            }

            pagesFetched++
            currentPage++
        }

        ApodSearchPage(
            items = collectedItems,
            page = (currentPage - 1).coerceAtLeast(startPage),
            totalPages = totalPages,
            totalResults = if (filter == SearchFilter.ALL) totalResults else collectedItems.size,
            searchTerm = term
        )
    }

    /**
     * Extrai data e título de "APOD: 2026 October 26 – Título". Resultados que não são
     * APOD (outros artigos de imagem do site) são descartados.
     */
    private fun parseSearchResult(result: SiteSearchResult): ApodResponse? {
        val title = HtmlCompat.fromHtml(result.title.orEmpty(), HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
            .trim()
        val match = SEARCH_TITLE_REGEX.find(title) ?: return null
        val date = try {
            LocalDate.parse(match.groupValues[1], SEARCH_TITLE_DATE_FORMAT)
        } catch (_: Exception) {
            return null
        }

        return ApodResponse(
            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            postId = result.id,
            title = match.groupValues[2].trim(),
            permalink = result.url,
            mediaType = "other"
        )
    }

    /**
     * Completa o resultado com os dados do APOD (imagem, explicação, créditos) e traduz.
     * Se o detalhe falhar, mantém título, data e link vindos da busca.
     */
    private suspend fun loadSearchResult(stub: ApodResponse): ApodResponse {
        val full = try {
            apiService.getApodByDate(dateCode = formatDateToCode(stub.date))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            stub
        }
        return translateApod(full)
    }

    /** Traduz título e explicação. Itens já traduzidos por completo (ex.: dados de contingência) voltam como estão. */
    suspend fun translateApod(apod: ApodResponse): ApodResponse {
        if (apod.isTranslated && !apod.translatedExplanation.isNullOrBlank()) return apod
        val cacheKey = if (apod.date.isNotBlank()) apod.date else apod.title
        translationCache[cacheKey]?.let { (translatedTitle, translatedExplanation) ->
            return apod.copy(
                translatedTitle = translatedTitle,
                translatedExplanation = translatedExplanation,
                isTranslated = true
            )
        }

        return try {
            val titlePt = translationService.translateText(apod.title)
            val explanationPt = translationService.translateText(apod.cleanExplanation)

            translationCache[cacheKey] = Pair(titlePt, explanationPt)

            apod.copy(
                translatedTitle = titlePt,
                translatedExplanation = explanationPt,
                isTranslated = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Em caso de falha de tradução, mantém o item original com fallback transparente
            apod.copy(isTranslated = false)
        }
    }

    /**
     * Traduz só o título (rápido), para o card aparecer em português enquanto a
     * explicação é traduzida em segundo plano. Até lá, o card mostra a explicação original.
     */
    suspend fun translateTitle(apod: ApodResponse): ApodResponse {
        if (apod.isTranslated && !apod.translatedTitle.isNullOrBlank()) return apod
        return try {
            apod.copy(translatedTitle = translationService.translateText(apod.title), isTranslated = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            apod
        }
    }

    private fun formatDateToCode(dateString: String): String {
        return try {
            val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            localDate.format(DateTimeFormatter.ofPattern("yyMMdd"))
        } catch (_: Exception) {
            dateString.replace("-", "").takeLast(6)
        }
    }

    companion object {
        const val SEARCH_PAGE_SIZE = 10

        private val NASA_APOD_ZONE: ZoneId = ZoneId.of("America/New_York")
        private val SEARCH_TITLE_REGEX = Regex("""^APOD:\s*(\d{4}\s+[A-Za-z]+\s+\d{1,2})\s*[–—-]\s*(.+)$""")
        private val SEARCH_TITLE_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy MMMM d", Locale.ENGLISH)

        /**
         * Data mais recente elegível para o NASA APOD: limita pela menor data entre o fuso local
         * do dispositivo e o fuso oficial de publicação da NASA (America/New_York), evitando datas
         * futuras (ex.: emuladores em UTC) ou ainda não publicadas.
         */
        fun latestApodDate(
            localToday: LocalDate = LocalDate.now(),
            nasaToday: LocalDate = LocalDate.now(NASA_APOD_ZONE)
        ): LocalDate = minOf(localToday, nasaToday)
    }
}
