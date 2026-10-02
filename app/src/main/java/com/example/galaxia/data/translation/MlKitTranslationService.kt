package com.example.galaxia.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do serviço de tradução utilizando Google ML Kit On-Device Translation.
 * Realiza traduções totalmente locais no dispositivo, sem custos e offline após o download inicial do modelo.
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
            // Garante que o modelo de idioma está disponível no dispositivo
            translator.downloadModelIfNeeded().awaitResult()
            // Traduz o texto de forma assíncrona no processador local
            translator.translate(text).awaitResult()
        } catch (e: Exception) {
            // Em caso de falha na tradução (ex: offline antes de baixar modelo), retorna o texto original (Fallback)
            text
        }
    }

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
