package com.example.galaxia.data.translation

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Armazenamento das traduções já feitas, usado por [CachedTranslationService].
 */
interface TranslationStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)

    companion object {
        /** Chave curta e estável para o texto (SHA-256), separada por par de idiomas. */
        fun keyOf(text: String, sourceLang: String, targetLang: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return "$sourceLang>$targetLang:" + digest.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Guarda as traduções em um arquivo JSON no armazenamento interno do app.
 *
 * O arquivo é lido uma vez, na primeira consulta, e as gravações são agrupadas (uma escrita
 * logo depois de uma sequência de traduções). Mantém só as [maxEntries] mais recentes.
 */
class FileTranslationStore(
    private val file: File,
    private val maxEntries: Int = 600
) : TranslationStore {

    constructor(context: Context) : this(File(context.filesDir, "translations_cache.json"))

    private val mutex = Mutex()
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    // Ordem de acesso: as traduções usadas há mais tempo saem primeiro quando passa do limite
    private var entries: LinkedHashMap<String, String>? = null

    override suspend fun get(key: String): String? = mutex.withLock { loaded()[key] }

    override suspend fun put(key: String, value: String) {
        mutex.withLock {
            val map = loaded()
            map[key] = value
            while (map.size > maxEntries) {
                map.remove(map.keys.first())
            }
        }
        scheduleSave()
    }

    private suspend fun loaded(): LinkedHashMap<String, String> {
        entries?.let { return it }
        val map = LinkedHashMap<String, String>(64, 0.75f, true)
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val json = AtomicFile(file).readFully().toString(Charsets.UTF_8)
                    val saved: Map<String, String>? = gson.fromJson(json, MAP_TYPE)
                    saved?.let { map.putAll(it) }
                }
            } catch (e: Exception) {
                // Arquivo corrompido: começa vazio, as traduções serão refeitas
                Log.w(TAG, "Não foi possível ler o cache de traduções", e)
            }
        }
        entries = map
        return map
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            val snapshot = mutex.withLock { LinkedHashMap(loaded()) }
            val atomicFile = AtomicFile(file)
            val stream = atomicFile.startWrite()
            try {
                stream.write(gson.toJson(snapshot).toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(stream)
            } catch (e: Exception) {
                atomicFile.failWrite(stream)
                Log.w(TAG, "Não foi possível salvar o cache de traduções", e)
            }
        }
    }

    private companion object {
        const val TAG = "TranslationStore"
        const val SAVE_DELAY_MS = 1_000L
        val MAP_TYPE = object : TypeToken<Map<String, String>>() {}.type
    }
}
