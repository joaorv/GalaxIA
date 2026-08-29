package com.example.galaxia.data.local

/**
 * Banco de dados local do GalaxIA (camada Data - local).
 *
 * Quando o Room for adicionado ao build.gradle.kts, esta classe
 * passará a estender androidx.room.RoomDatabase e centralizar o
 * acesso aos DAOs responsáveis por persistir os itens favoritados
 * pelo usuário (ex.: fotos do APOD salvas localmente).
 */
abstract class AppDatabase
