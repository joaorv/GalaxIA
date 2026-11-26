package com.example.galaxia.data.local

import android.content.Context
import com.example.galaxia.data.model.SpaceNewsItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class CachedCategoryData(
    val items: List<SpaceNewsItem>,
    val cachedAtMillis: Long,
    val pageLoaded: Int,
    val hasMore: Boolean,
    val pagingWindowIndex: Int = 0,
    val pagingNextApiPage: Int = 1,
    val pagingExhausted: Boolean = false
)

/**
 * Cache temporário para notícias e curiosidades indexado por chave (categoria ou categoria:subtópico),
 * com suporte a fallback offline.
 */
interface SpaceNewsCache {
    suspend fun getValid(
        cacheKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ): CachedCategoryData?

    suspend fun getStale(cacheKey: String): CachedCategoryData?

    suspend fun put(cacheKey: String, data: CachedCategoryData)
}

/**
 * Implementação em memória + disco usando arquivo JSON no diretório de cache do app.
 * Seguro para concorrência (Mutex) e para execução em testes unitários JVM.
 */
class FileSpaceNewsCache(
    private val file: File,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) : SpaceNewsCache {

    constructor(context: Context) : this(File(context.cacheDir, CACHE_FILE_NAME))

    private val mutex = Mutex()
    private val gson = Gson()
    private var entries: MutableMap<String, CachedCategoryData>? = null

    override suspend fun getValid(cacheKey: String, nowMillis: Long): CachedCategoryData? {
        val data = mutex.withLock { loaded()[cacheKey] } ?: return null
        return if (nowMillis - data.cachedAtMillis <= ttlMillis) data else null
    }

    override suspend fun getStale(cacheKey: String): CachedCategoryData? {
        return mutex.withLock { loaded()[cacheKey] }
    }

    override suspend fun put(cacheKey: String, data: CachedCategoryData) {
        val snapshot = mutex.withLock {
            val map = loaded()
            map[cacheKey] = data
            HashMap(map)
        }
        persistToDisk(snapshot)
    }

    private suspend fun loaded(): MutableMap<String, CachedCategoryData> {
        entries?.let { return it }
        val loadedMap = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext mutableMapOf()
            try {
                val json = file.readText(Charsets.UTF_8)
                val parsed: Map<String, CachedCategoryData>? = gson.fromJson(json, CACHE_MAP_TYPE)
                sanitizeCacheMap(parsed)
            } catch (_: Exception) {
                mutableMapOf()
            }
        }
        entries = loadedMap
        return loadedMap
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun sanitizeCacheMap(
        raw: Map<String, CachedCategoryData>?
    ): MutableMap<String, CachedCategoryData> {
        if (raw == null) return mutableMapOf()
        val result = mutableMapOf<String, CachedCategoryData>()
        for ((key, data) in raw) {
            if (data == null || data.items == null) continue
            val cleanItems = data.items.mapNotNull { item ->
                if (item == null ||
                    item.id == null ||
                    item.category == null ||
                    item.title == null ||
                    item.sourceUrl == null
                ) {
                    null
                } else {
                    item.copy(
                        galleryImageUrls = item.safeGalleryImageUrls,
                        metadataChips = item.safeMetadataChips
                    )
                }
            }
            if (cleanItems.isNotEmpty()) {
                result[key] = data.copy(items = cleanItems)
            }
        }
        return result
    }

    private suspend fun persistToDisk(snapshot: Map<String, CachedCategoryData>) {
        withContext(Dispatchers.IO) {
            try {
                file.parentFile?.mkdirs()
                val tmpFile = File(file.parentFile, "${file.name}.tmp")
                tmpFile.writeText(gson.toJson(snapshot), Charsets.UTF_8)
                if (file.exists()) {
                    file.delete()
                }
                if (!tmpFile.renameTo(file)) {
                    file.writeText(gson.toJson(snapshot), Charsets.UTF_8)
                    tmpFile.delete()
                }
            } catch (_: Exception) {
                // Falha de gravação em cache não deve interromper o fluxo do usuário
            }
        }
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
        private const val CACHE_FILE_NAME = "space_news_cache_v4.json"
        private val CACHE_MAP_TYPE = object : TypeToken<Map<String, CachedCategoryData>>() {}.type
    }
}
