package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
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
    /**
     * URL de la imagen del producto.
     * 
     * Este campo almacena la URL pública de la imagen subida a Supabase Storage.
     * Formato: {supabaseUrl}/storage/v1/object/public/{bucketName}/ofertas/{offerId}.{extension}
     * 
     * Cuando el bodeguero crea una oferta:
     * 1. Selecciona una imagen desde la galería o cámara (URI local: content:// o file://)
     * 2. La imagen se sube a Supabase Storage usando uploadOfferImage()
     * 3. Se obtiene la URL pública de Supabase Storage
     * 4. Esta URL se guarda en este campo en la base de datos
     * 5. Los clientes pueden ver la imagen usando esta URL pública
     * 
     * Si no se proporciona imagen, se usa un placeholder por defecto.
     */
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
) : Parcelable {
    val progress: Float
        get() {
            if (targetUnits <= 0) return 0f
            return (reservedUnits.toFloat() / targetUnits.toFloat()).coerceIn(0f, 1f)
        }
    
    val unitsNeeded: Int
        get() = (targetUnits - reservedUnits).coerceAtLeast(0)
    
    val isExpired: Boolean
        get() {
            val expiresAtLong = expiresAt?.let { 
                try {
                    java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            } ?: return false
            return System.currentTimeMillis() > expiresAtLong || status == OfferStatus.EXPIRED
        }
    
    val timeRemaining: Long
        get() {
            val expiresAtLong = expiresAt?.let { 
                try {
                    java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            } ?: return 0L
            return maxOf(0, expiresAtLong - System.currentTimeMillis())
        }
}

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

