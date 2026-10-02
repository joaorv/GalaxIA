package com.example.galaxia.data.translation

/**
 * Contrato abstrato para serviço de tradução de textos.
 */
interface TranslationService {
    /**
     * Traduz o texto fornecido para o idioma alvo (padrão: Português).
     */
    suspend fun translateText(text: String, sourceLang: String = "en", targetLang: String = "pt"): String
}
