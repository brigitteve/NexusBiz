package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class User(
    @SerialName("id")
    val id: String = "",

    @SerialName("alias")
    val alias: String = "",

    @SerialName("password_hash")
    val passwordHash: String = "",

    @SerialName("fecha_nacimiento")
    val fechaNacimiento: String = "",

    @SerialName("district")
    val district: String = "",

    @SerialName("email")
    val email: String? = null,

    @SerialName("avatar")
    val avatar: String? = null,

    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    @SerialName("points")
    val points: Int = 0,

    @SerialName("tier")
    val tier: UserTier = UserTier.BRONZE,

    @SerialName("badges")
    val badges: List<String> = emptyList(),

    @SerialName("streak")
    val streak: Int = 0,

    @SerialName("completed_groups")
    val completedGroups: Int = 0,

    @SerialName("total_savings")
    val totalSavings: Double = 0.0,

    @SerialName("user_type")
    val userType: UserType = UserType.CONSUMER,

    @SerialName("created_at")
    val createdAt: String? = null
) : Parcelable {
    /**
     * Calcula el tier basado en los puntos acumulados
     */
    fun calculateTier(): UserTier {
        return when {
            points >= 200 -> UserTier.GOLD
            points >= 100 -> UserTier.SILVER
            else -> UserTier.BRONZE
        }
    }
    
    /**
     * Obtiene el máximo de unidades que puede reservar según su tier
     */
    fun maxReservationUnits(): Int {
        return when (calculateTier()) {
            UserTier.BRONZE -> 2
            UserTier.SILVER -> 4
            UserTier.GOLD -> 6
        }
    }
}

@Serializable
enum class UserTier {
    @SerialName("BRONZE")
    BRONZE,
    @SerialName("SILVER")
    SILVER,
    @SerialName("GOLD")
    GOLD
}

@Serializable
enum class UserType {
    @SerialName("CONSUMER")
    CONSUMER,
    @SerialName("STORE_OWNER")
    STORE_OWNER
}

