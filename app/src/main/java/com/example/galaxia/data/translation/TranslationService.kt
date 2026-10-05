package com.example.galaxia.data.translation

/**
 * Contrato de serviço para tradução de conteúdos astronômicos da NASA.
 * Permite desacoplar o mecanismo de tradução (ex: Google ML Kit, Cloud Translation API ou Mock)
 * da camada de repositório e regras de negócio do GalaxIA.
 */
interface TranslationService {

    /**
     * Traduz um texto de forma assíncrona do idioma de origem para o idioma de destino.
     *
     * @param text Texto em inglês a ser traduzido.
     * @param sourceLang Código ISO do idioma de origem (padrão: "en").
     * @param targetLang Código ISO do idioma de destino (padrão: "pt").
     * @return O texto traduzido em português ou o texto original em caso de falha de conexão ou modelo.
     */
    suspend fun translateText(text: String, sourceLang: String = "en", targetLang: String = "pt"): String
}
