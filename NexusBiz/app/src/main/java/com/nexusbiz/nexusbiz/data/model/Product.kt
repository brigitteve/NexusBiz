package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Product(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("category_name")
    val category: String? = null,
    @SerialName("normal_price")
    val normalPrice: Double = 0.0,
    @SerialName("group_price")
    val groupPrice: Double = 0.0,
    @SerialName("min_group_size")
    val minGroupSize: Int = 3,
    @SerialName("max_group_size")
    val maxGroupSize: Int = 10,
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("district")
    val district: String = "",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("duration_hours")
    val durationHours: Int = 24,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) : Parcelable

