package com.example.galaxia

import com.example.galaxia.data.local.ApiKeyStorage
import com.example.galaxia.data.local.NasaApiKeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NasaApiKeyManagerTest {

    private class MemoryApiKeyStorage(
        private var key: String? = null,
        private var dismissed: Boolean = false
    ) : ApiKeyStorage {
        override fun getCustomKey(): String? = key
        override fun setCustomKey(key: String?) { this.key = key }
        override fun isPromptDismissed(): Boolean = dismissed
        override fun setPromptDismissed(dismissed: Boolean) { this.dismissed = dismissed }
    }

    @Test
    fun initialState_defaultsToDemoKeyAndShowsOnboardingPrompt() {
        val manager = NasaApiKeyManager(MemoryApiKeyStorage())

        assertEquals(NasaApiKeyManager.DEMO_KEY, manager.getActiveApiKey())
        assertNull(manager.customApiKey.value)
        assertFalse(manager.isUsingCustomKey.value)
        assertTrue(manager.shouldShowOnboardingPrompt.value)
    }

    @Test
    fun dismissPrompt_keepsDemoKeyAndHidesPrompt() {
        val storage = MemoryApiKeyStorage()
        val manager = NasaApiKeyManager(storage)

        manager.dismissPrompt()

        assertEquals(NasaApiKeyManager.DEMO_KEY, manager.getActiveApiKey())
        assertFalse(manager.isUsingCustomKey.value)
        assertFalse(manager.shouldShowOnboardingPrompt.value)
        assertTrue(storage.isPromptDismissed())
    }

    @Test
    fun saveApiKey_trimsValidKeyActivatesItAndRejectsBlankOrWhitespaceKeys() {
        val manager = NasaApiKeyManager(MemoryApiKeyStorage())

        assertFalse(manager.saveApiKey("   "))
        assertFalse(manager.saveApiKey("abc def"))
        assertEquals(NasaApiKeyManager.DEMO_KEY, manager.getActiveApiKey())

        assertTrue(manager.saveApiKey("  my_nasa_key_123  "))
        assertEquals("my_nasa_key_123", manager.getActiveApiKey())
        assertEquals("my_nasa_key_123", manager.customApiKey.value)
        assertTrue(manager.isUsingCustomKey.value)
        assertFalse(manager.shouldShowOnboardingPrompt.value)
    }

    @Test
    fun useDemoKey_clearsCustomKeyAndRevertsToDemoKey() {
        val manager = NasaApiKeyManager(MemoryApiKeyStorage(key = "existing_key", dismissed = true))
        assertTrue(manager.isUsingCustomKey.value)

        manager.useDemoKey()

        assertEquals(NasaApiKeyManager.DEMO_KEY, manager.getActiveApiKey())
        assertNull(manager.customApiKey.value)
        assertFalse(manager.isUsingCustomKey.value)
        assertFalse(manager.shouldShowOnboardingPrompt.value)
    }
}
