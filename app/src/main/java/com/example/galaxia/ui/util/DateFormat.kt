package com.example.galaxia.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val brazilianDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Converte uma data ISO da API (yyyy-MM-dd) para o formato brasileiro (dd/MM/yyyy).
 * Se o texto não for uma data válida, retorna o valor original.
 */
fun formatDateBr(dateString: String): String {
    return try {
        LocalDate.parse(dateString).format(brazilianDateFormatter)
    } catch (_: Exception) {
        dateString
    }
}
