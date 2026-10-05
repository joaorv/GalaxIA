package com.example.galaxia.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do serviço de tradução utilizando o Google ML Kit On-Device Translation.
 *
 * Características de Arquitetura:
 * - Execução local no processador do dispositivo (sem custo de API de tradução).
 * - Operação offline após o download do pacote de idioma (EN ➔ PT).
 * - Tratamento de falhas resiliente: preserva a experiência do usuário retornando o texto original.
 */
class MlKitTranslationService : TranslationService {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.PORTUGUESE)
        .build()

    private val translator = Translation.getClient(options)

    override suspend fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        if (text.isBlank()) return text

        return try {
            // Garante a disponibilidade do modelo de linguagem no armazenamento local
            translator.downloadModelIfNeeded().awaitResult()
            // Realiza a tradução no processador do aparelho
            translator.translate(text).awaitResult()
        } catch (e: Exception) {
            // Estratégia de Fallback: devolve o texto original sem travar a interface
            text
        }
    }

    /**
     * Converte o padrão de escuta por Callbacks do Task em Coroutines do Kotlin.
     */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}
