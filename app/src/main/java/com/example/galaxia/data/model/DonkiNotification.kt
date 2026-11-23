package com.example.galaxia.data.model

/**
 * Notificação de clima espacial do DONKI (api.nasa.gov/DONKI/notifications).
 *
 * [messageType] pode ser FLR (erupção solar), SEP (partículas energéticas), CME
 * (ejeção de massa coronal), IPS (choque interplanetário), MPC (cruzamento da
 * magnetopausa), GST (tempestade geomagnética), RBE (cinturão de radiação) ou
 * Report (resumo semanal).
 */
data class DonkiNotification(
    val messageType: String? = null,
    val messageID: String? = null,
    val messageURL: String? = null,
    /** Data/hora de emissão no formato "2026-11-10T13:06Z". */
    val messageIssueTime: String? = null,
    /** Texto completo em inglês, com cabeçalho, seção "## Summary:" e "## Notes:". */
    val messageBody: String? = null
)
