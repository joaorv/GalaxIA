package com.example.galaxia.data.translation

import com.example.galaxia.GalaxiaApplication

/**
 * Serviço de tradução compartilhado pelo app: ML Kit com cache em disco.
 * Os repositórios usam esta instância por padrão, para que todos aproveitem o mesmo cache.
 */
object AppTranslation {
    val service: TranslationService by lazy {
        CachedTranslationService(
            delegate = MlKitTranslationService(),
            store = FileTranslationStore(GalaxiaApplication.instance)
        )
    }
}
