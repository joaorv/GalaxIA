package com.example.galaxia.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do serviço de tradução utilizando o Google ML Kit On-Device Translation.
 * Realiza traduções totalmente locais no dispositivo, sem custos e offline após o download inicial.
 */
class MlKitTranslationService : TranslationService {

    // Um tradutor por par de idiomas (ex.: en→pt para o conteúdo, pt→en para os termos de busca).
    // O modelo de português é o mesmo nos dois sentidos, então não há download extra.
    private val translators = ConcurrentHashMap<Pair<String, String>, Translator>()

    private fun translatorFor(sourceLang: String, targetLang: String): Translator? {
        val source = TranslateLanguage.fromLanguageTag(sourceLang) ?: return null
        val target = TranslateLanguage.fromLanguageTag(targetLang) ?: return null
        return translators.getOrPut(source to target) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
            )
        }
    }

    override suspend fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        if (text.isBlank() || sourceLang == targetLang) return text
        val translator = translatorFor(sourceLang, targetLang) ?: return text

        return try {
            // Garante que o modelo de idioma (EN -> PT) está baixado
            translator.downloadModelIfNeeded().awaitResult()
            // Executa a tradução no processador local
            translator.translate(text).awaitResult()
        } catch (e: Exception) {
            // Fallback transparente: retorna o texto original em inglês em caso de falha
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
