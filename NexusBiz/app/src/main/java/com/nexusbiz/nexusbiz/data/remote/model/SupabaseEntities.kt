package com.nexusbiz.nexusbiz.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: String = "",
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("alias")
    val alias: String = "",
    @SerialName("email")
    val email: String? = null,
    @SerialName("avatar")
    val avatar: String? = null,
    @SerialName("password_hash")
    val passwordHash: String? = null,
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
    @SerialName("district")
    val district: String = "",
    @SerialName("fecha_nacimiento")
    val fechaNacimiento: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class UserType {
    @SerialName("CONSUMER")
    CONSUMER,
    @SerialName("STORE_OWNER")
    STORE_OWNER
}

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
    val categoryName: String = "",
    @SerialName("normal_price")
    val normalPrice: Double = 0.0,
    @SerialName("group_price")
    val groupPrice: Double = 0.0,
    @SerialName("min_group_size")
    val minGroupSize: Int = 0,
    @SerialName("max_group_size")
    val maxGroupSize: Int = 0,
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("district")
    val district: String = "",
    @SerialName("is_active")
    val isActive: Boolean = false,
    @SerialName("duration_hours")
    val durationHours: Int = 0,
    @SerialName("store_plan")
    val storePlan: String? = null, // "FREE" o "PRO" (denormalizado desde bodegas)
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

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
    @SerialName("owner_id")
    val ownerId: String = "",
    @SerialName("rating")
    val rating: Double = 0.0,
    @SerialName("total_sales")
    val totalSales: Int = 0,
    @SerialName("ruc")
    val ruc: String = "",
    @SerialName("commercial_name")
    val commercialName: String = "",
    @SerialName("owner_alias")
    val ownerAlias: String? = null,
    @SerialName("plan")
    val plan: String = "FREE", // "FREE" o "PRO"
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Group(
    @SerialName("id")
    val id: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("product_image")
    val productImage: String = "",
    @SerialName("creator_id")
    val creatorId: String = "",
    @SerialName("creator_alias")
    val creatorAlias: String = "",
    @SerialName("current_size")
    val currentSize: Int = 0,
    @SerialName("target_size")
    val targetSize: Int = 0,
    @SerialName("status")
    val status: GroupStatus = GroupStatus.ACTIVE,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("qr_code")
    val qrCode: String = "",
    @SerialName("validated_at")
    val validatedAt: String? = null,
    @SerialName("normal_price")
    val normalPrice: Double = 0.0,
    @SerialName("group_price")
    val groupPrice: Double = 0.0
)

@Serializable
enum class GroupStatus {
    @SerialName("ACTIVE")
    ACTIVE,
    @SerialName("PICKUP")
    PICKUP,
    @SerialName("VALIDATED")
    VALIDATED,
    @SerialName("COMPLETED")
    COMPLETED,
    @SerialName("EXPIRED")
    EXPIRED
}

@Serializable
data class Participant(
    @SerialName("id")
    val id: String = "",
    @SerialName("group_id")
    val groupId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("alias")
    val alias: String = "",
    @SerialName("avatar")
    val avatar: String = "",
    @SerialName("reserved_units")
    val reservedUnits: Int = 1,
    @SerialName("joined_at")
    val joinedAt: String? = null,
    @SerialName("is_validated")
    val isValidated: Boolean = false,
    @SerialName("validated_at")
    val validatedAt: String? = null,
    @SerialName("status")
    val status: String = "RESERVED"
)

@Serializable
data class Category(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("icon")
    val icon: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class VerificationCode(
    @SerialName("id")
    val id: String = "",
    @SerialName("phone")
    val phone: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("attempts")
    val attempts: Int = 0,
    @SerialName("is_used")
    val isUsed: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
