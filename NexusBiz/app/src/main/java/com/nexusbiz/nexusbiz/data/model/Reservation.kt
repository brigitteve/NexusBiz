package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Reservation(
    @SerialName("id")
    val id: String = "",
    @SerialName("offer_id")
    val offerId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("units")
    val units: Int = 1,
    @SerialName("total_price")
    val totalPrice: Double = 0.0,
    @SerialName("level_snapshot")
    val levelSnapshot: GamificationLevel = GamificationLevel.BRONCE,
    @SerialName("status")
    val status: ReservationStatus = ReservationStatus.RESERVED,
    @SerialName("reserved_at")
    val reservedAt: String? = null,
    @SerialName("validated_at")
    val validatedAt: String? = null,
    // Campos denormalizados desde vista reservas_completas
    @SerialName("user_alias")
    val userAlias: String? = null,
    @SerialName("user_avatar")
    val userAvatar: String? = null,
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("product_image")
    val productImage: String? = null,
    @SerialName("store_name")
    val storeName: String? = null,
    @SerialName("pickup_address")
    val pickupAddress: String? = null
) : Parcelable {
    val isValidated: Boolean
        get() = status == ReservationStatus.VALIDATED
}

@Serializable
enum class GamificationLevel {
    @SerialName("BRONCE")
    BRONCE,
    @SerialName("PLATA")
    PLATA,
    @SerialName("ORO")
    ORO
}

