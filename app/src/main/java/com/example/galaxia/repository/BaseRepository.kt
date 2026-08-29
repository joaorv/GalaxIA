package com.example.galaxia.repository

/**
 * Contrato base dos Repositories do GalaxIA (camada MVVM).
 *
 * Os repositories concretos (ex.: ApodRepository) vão orquestrar o
 * acesso aos dados combinando a fonte remota (data.remote.ApiService,
 * via Retrofit) com a fonte local (data.local.AppDatabase, via Room),
 * expondo para os ViewModels uma API única e agnóstica de origem dos
 * dados.
 */
interface BaseRepository
