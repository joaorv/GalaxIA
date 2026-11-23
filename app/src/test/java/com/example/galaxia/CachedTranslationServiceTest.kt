package com.example.galaxia

import com.example.galaxia.data.translation.CachedTranslationService
import com.example.galaxia.data.translation.TranslationService
import com.example.galaxia.data.translation.TranslationStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CachedTranslationServiceTest {

    private class MemoryStore : TranslationStore {
        val map = mutableMapOf<String, String>()
        override suspend fun get(key: String) = map[key]
        override suspend fun put(key: String, value: String) { map[key] = value }
    }

    /** Tradutor falso e lento, que conta quantas vezes foi chamado. */
    private class SlowTranslator(private val fail: Boolean = false) : TranslationService {
        val calls = AtomicInteger()
        override suspend fun translateText(text: String, sourceLang: String, targetLang: String): String {
            calls.incrementAndGet()
            delay(100)
            return if (fail) text else "PT($text)"
        }
    }

    @Test
    fun secondCall_usesCache() = runBlocking {
        val delegate = SlowTranslator()
        val service = CachedTranslationService(delegate, MemoryStore())

        assertEquals("PT(Nebula)", service.translateText("Nebula"))
        assertEquals("PT(Nebula)", service.translateText("Nebula"))
        assertEquals(1, delegate.calls.get())
    }

    @Test
    fun concurrentIdenticalRequests_translateOnce() = runBlocking {
        val delegate = SlowTranslator()
        val service = CachedTranslationService(delegate, MemoryStore())

        val results = List(5) { async { service.translateText("Same text") } }.awaitAll()

        assertEquals(List(5) { "PT(Same text)" }, results)
        assertEquals(1, delegate.calls.get())
    }

    @Test
    fun failedTranslation_isNotCached() = runBlocking {
        val store = MemoryStore()
        val service = CachedTranslationService(SlowTranslator(fail = true), store)

        assertEquals("Galaxy", service.translateText("Galaxy"))
        assertEquals(0, store.map.size)
    }

    @Test
    fun cacheKey_dependsOnLanguagePair() {
        assertNotEquals(
            TranslationStore.keyOf("lua", "pt", "en"),
            TranslationStore.keyOf("lua", "en", "pt")
        )
    }
}
