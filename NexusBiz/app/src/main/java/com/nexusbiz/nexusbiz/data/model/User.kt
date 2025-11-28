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
) : Parcelable

@Serializable
enum class UserType {
    @SerialName("CONSUMER")
    CONSUMER,
    @SerialName("STORE_OWNER")
    STORE_OWNER
}

