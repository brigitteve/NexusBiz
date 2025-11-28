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
    @SerialName("gamification_level")
    val gamificationLevel: com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel? = null,
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
    @SerialName("plan_type")
    val plan: String = "FREE", // "FREE" o "PRO" - mapea a plan_type en BD
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
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

@Serializable
data class Offer(
    @SerialName("id")
    val id: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("product_key")
    val productKey: String = "",
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("normal_price")
    val normalPrice: Double = 0.0,
    @SerialName("group_price")
    val groupPrice: Double = 0.0,
    @SerialName("target_units")
    val targetUnits: Int = 3,
    @SerialName("reserved_units")
    val reservedUnits: Int = 0,
    @SerialName("validated_units")
    val validatedUnits: Int = 0,
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("district")
    val district: String = "",
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("pickup_address")
    val pickupAddress: String = "",
    @SerialName("status")
    val status: OfferStatus = OfferStatus.ACTIVE,
    @SerialName("duration_hours")
    val durationHours: Int = 24,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class OfferStatus {
    @SerialName("ACTIVE")
    ACTIVE,
    @SerialName("PICKUP")
    PICKUP,
    @SerialName("COMPLETED")
    COMPLETED,
    @SerialName("EXPIRED")
    EXPIRED
}

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
    val status: ReservationStatusRemote = ReservationStatusRemote.RESERVED,
    @SerialName("reserved_at")
    val reservedAt: String? = null,
    @SerialName("validated_at")
    val validatedAt: String? = null,
    // Campos de la vista reservas_completas
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
)

@Serializable
enum class GamificationLevel {
    @SerialName("BRONCE")
    BRONCE,
    @SerialName("PLATA")
    PLATA,
    @SerialName("ORO")
    ORO
}

@Serializable
enum class ReservationStatusRemote {
    @SerialName("RESERVED")
    RESERVED,
    @SerialName("VALIDATED")
    VALIDATED,
    @SerialName("EXPIRED")
    EXPIRED,
    @SerialName("CANCELLED")
    CANCELLED
}
