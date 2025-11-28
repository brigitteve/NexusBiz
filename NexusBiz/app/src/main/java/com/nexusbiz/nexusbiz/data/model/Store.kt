package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Store(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("address")
    val address: String = "",
    @SerialName("district")
    val district: String = "",
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("has_stock")
    val hasStock: Boolean = false,
    @SerialName("distance")
    val distance: Double = 0.0, // en km (calculado, no en BD)
    @SerialName("owner_id")
    val ownerId: String = "",
    @SerialName("rating")
    val rating: Double? = null,
    @SerialName("total_sales")
    val totalSales: Int = 0,
    @SerialName("ruc")
    val ruc: String? = null,
    @SerialName("commercial_name")
    val commercialName: String? = null,
    @SerialName("owner_alias")
    val ownerAlias: String? = null,
    @SerialName("plan")
    val plan: StorePlan = StorePlan.FREE,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) : Parcelable

@Serializable
enum class StorePlan {
    @SerialName("FREE")
    FREE,
    @SerialName("PRO")
    PRO
}
