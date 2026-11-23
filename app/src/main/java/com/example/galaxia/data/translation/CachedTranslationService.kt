package com.example.galaxia.data.translation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

/**
 * Acrescenta cache persistente a um [TranslationService].
 *
 * A tradução no aparelho leva alguns segundos por parágrafo e o feed mostra quase sempre
 * os mesmos textos (a lista do APOD muda uma vez por dia), então guardar o resultado evita
 * traduzir tudo de novo a cada abertura do app.
 *
 * Também junta pedidos iguais feitos ao mesmo tempo (ex.: o feed e o histórico traduzindo
 * a mesma foto do dia) em uma única tradução. A tradução roda no escopo deste serviço, então
 * continua e é guardada mesmo que a tela que pediu seja cancelada.
 */
class CachedTranslationService(
    private val delegate: TranslationService,
    private val store: TranslationStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TranslationService {

    private val inFlight = ConcurrentHashMap<String, Deferred<String>>()

    override suspend fun translateText(text: String, sourceLang: String, targetLang: String): String {
        if (text.isBlank() || sourceLang == targetLang) return text

        val key = TranslationStore.keyOf(text, sourceLang, targetLang)
        store.get(key)?.let { return it }

        val deferred = inFlight.computeIfAbsent(key) {
            // LAZY: só começa depois de registrado no mapa, para a remoção no fim não correr antes
            scope.async(start = CoroutineStart.LAZY) {
                try {
                    val result = delegate.translateText(text, sourceLang, targetLang)
                    // O serviço devolve o próprio texto quando falha: isso não vai para o cache
                    if (result != text) store.put(key, result)
                    result
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        deferred.start()
        return deferred.await()
    }
}
