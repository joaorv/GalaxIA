package com.example.galaxia.data.model

/**
 * Marcador base para os modelos de dados do GalaxIA.
 *
 * Os modelos concretos (ex.: Apod, representando a resposta do
 * endpoint APOD da NASA) devem implementar esta interface. Quando o
 * Room for adicionado ao projeto, os modelos persistidos localmente
 * (favoritos) também poderão receber as anotações @Entity aqui.
 */
interface BaseModel
