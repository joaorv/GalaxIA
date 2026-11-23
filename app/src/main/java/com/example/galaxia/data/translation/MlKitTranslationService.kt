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
 *
 * O ML Kit traduz um texto por vez, então os tradutores são compartilhados pelo app inteiro
 * (uma única fila e um único modelo carregado em memória, em vez de um por repositório).
 * Use [AppTranslation.service], que acrescenta cache em disco por cima desta classe.
 */
class MlKitTranslationService : TranslationService {

    override suspend fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        if (text.isBlank() || sourceLang == targetLang) return text
        val key = translatorKey(sourceLang, targetLang) ?: return text
        val translator = translatorFor(key)

        return try {
            // Garante que o modelo de idioma está baixado; depois da primeira vez não verifica de novo
            if (key !in readyModels) {
                translator.downloadModelIfNeeded().awaitResult()
                readyModels += key
            }
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

    private companion object {
        // Um tradutor por par de idiomas (ex.: en→pt para o conteúdo, pt→en para os termos de busca).
        // O modelo de português é o mesmo nos dois sentidos, então não há download extra.
        val translators = ConcurrentHashMap<Pair<String, String>, Translator>()

        // Pares cujo modelo já foi confirmado como baixado
        val readyModels: MutableSet<Pair<String, String>> = ConcurrentHashMap.newKeySet()

        fun translatorKey(sourceLang: String, targetLang: String): Pair<String, String>? {
            val source = TranslateLanguage.fromLanguageTag(sourceLang) ?: return null
            val target = TranslateLanguage.fromLanguageTag(targetLang) ?: return null
            return source to target
        }

        fun translatorFor(key: Pair<String, String>): Translator = translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(key.first)
                    .setTargetLanguage(key.second)
                    .build()
            )
        }
    }
}
