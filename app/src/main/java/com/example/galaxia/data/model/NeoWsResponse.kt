package com.example.galaxia.data.model

import com.google.gson.annotations.SerializedName

/**
 * Resposta do endpoint NeoWs "feed" (api.nasa.gov/neo/rest/v1/feed): asteroides
 * que passam perto da Terra no intervalo de datas consultado, agrupados por data.
 *
 * Todos os campos são anuláveis porque o Gson ignora os valores padrão do Kotlin.
 * A API envia distâncias e velocidades como texto, por isso são String aqui.
 */
data class NeoFeedResponse(
    @SerializedName("element_count")
    val elementCount: Int? = null,
    @SerializedName("near_earth_objects")
    val nearEarthObjects: Map<String, List<NearEarthObject>>? = null
)

data class NearEarthObject(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("nasa_jpl_url")
    val nasaJplUrl: String? = null,
    @SerializedName("estimated_diameter")
    val estimatedDiameter: EstimatedDiameter? = null,
    @SerializedName("is_potentially_hazardous_asteroid")
    val isPotentiallyHazardous: Boolean? = null,
    @SerializedName("close_approach_data")
    val closeApproachData: List<CloseApproachData>? = null
) {
    /** Nome sem os parênteses que a API usa nas designações provisórias, ex.: "(2005 WD)". */
    val displayName: String
        get() = name.orEmpty().trim().removeSurrounding("(", ")").ifBlank { "desconhecido" }
}

data class EstimatedDiameter(
    val meters: DiameterRange? = null
)

data class DiameterRange(
    @SerializedName("estimated_diameter_min")
    val min: Double? = null,
    @SerializedName("estimated_diameter_max")
    val max: Double? = null
)

data class CloseApproachData(
    @SerializedName("close_approach_date")
    val date: String? = null,
    /** Data e hora UTC no formato "2026-Nov-16 14:20". */
    @SerializedName("close_approach_date_full")
    val dateFull: String? = null,
    @SerializedName("relative_velocity")
    val relativeVelocity: RelativeVelocity? = null,
    @SerializedName("miss_distance")
    val missDistance: MissDistance? = null,
    @SerializedName("orbiting_body")
    val orbitingBody: String? = null
)

data class RelativeVelocity(
    @SerializedName("kilometers_per_hour")
    val kilometersPerHour: String? = null,
    @SerializedName("kilometers_per_second")
    val kilometersPerSecond: String? = null
)

data class MissDistance(
    val kilometers: String? = null,
    /** Distância em "distâncias lunares" (1 = distância média Terra–Lua). */
    val lunar: String? = null
)
