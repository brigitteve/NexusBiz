package com.nexusbiz.nexusbiz.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
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
    @SerialName("participants")
    val participants: List<Participant> = emptyList(), // Se carga desde tabla participantes
    @SerialName("current_size")
    val currentSize: Int = 0,
    @SerialName("target_size")
    val targetSize: Int = 3,
    @SerialName("status")
    val status: GroupStatus = GroupStatus.ACTIVE,
    @SerialName("expires_at")
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 horas (timestamp en BD)
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("qr_code")
    val qrCode: String = "",
    @SerialName("validated_at")
    val validatedAt: Long? = null,
    @SerialName("normal_price")
    val normalPrice: Double = 0.0,
    @SerialName("group_price")
    val groupPrice: Double = 0.0
) : Parcelable {
    val activeParticipants: List<Participant>
        get() = participants.filter { it.status != ReservationStatus.CANCELLED }

    val reservedUnits: Int
        get() {
            // IMPORTANTE: Usar currentSize directamente de Supabase
            // El trigger update_group_current_size() mantiene este valor actualizado
            // calculando SUM(reserved_units) de participantes con status != 'CANCELLED'
            // No recalcular desde participantes porque puede haber desfase
            return currentSize.coerceAtLeast(0)
        }
    
    /**
     * Obtiene las unidades reservadas excluyendo al creador del grupo (bodeguero)
     */
    fun getReservedUnitsExcludingCreator(storeOwnerId: String? = null): Int {
        val realParticipants = if (storeOwnerId != null) {
            activeParticipants.filter { it.userId != storeOwnerId && it.userId != creatorId }
        } else {
            activeParticipants.filter { it.userId != creatorId }
        }
        return realParticipants.sumOf { it.reservedUnits.coerceAtLeast(0) }
    }

    val progress: Float
        get() {
            // Progreso = current_size / target_size
            // Usar currentSize de Supabase (mantenido por trigger)
            if (targetSize <= 0) return 0f
            return (currentSize.toFloat() / targetSize.toFloat()).coerceIn(0f, 1f)
        }
    
    /**
     * Calcula unidades faltantes: target_size - current_size
     */
    val unitsNeeded: Int
        get() = (targetSize - currentSize).coerceAtLeast(0)

    val participantCount: Int
        get() = activeParticipants.size

    val validatedCount: Int
        get() = activeParticipants.count { it.status == ReservationStatus.VALIDATED }

    val pendingValidationCount: Int
        get() = (participantCount - validatedCount).coerceAtLeast(0)
    
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt || status == GroupStatus.EXPIRED
    
    val timeRemaining: Long
        get() = maxOf(0, expiresAt - System.currentTimeMillis())
    
    /**
     * Verifica si el grupo tiene participantes reales (clientes), excluyendo al creador si es bodeguero
     */
    fun hasRealParticipants(storeOwnerId: String? = null): Boolean {
        if (storeOwnerId == null) {
            // Si no hay storeOwnerId, considerar que tiene participantes si hay al menos uno activo
            return activeParticipants.isNotEmpty()
        }
        // Filtrar participantes que no sean el dueño de la bodega
        val realParticipants = activeParticipants.filter { it.userId != storeOwnerId && it.userId != creatorId }
        return realParticipants.isNotEmpty() || (activeParticipants.any { it.userId != storeOwnerId })
    }
    
    /**
     * Obtiene el número de participantes reales (clientes), excluyendo al bodeguero
     */
    fun getRealParticipantCount(storeOwnerId: String? = null): Int {
        if (storeOwnerId == null) {
            return activeParticipants.size
        }
        return activeParticipants.count { it.userId != storeOwnerId && it.userId != creatorId }
    }
}

@Parcelize
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
    val joinedAt: Long = System.currentTimeMillis(),
    @SerialName("is_validated")
    val isValidated: Boolean = false,
    @SerialName("validated_at")
    val validatedAt: Long? = null,
    @SerialName("status")
    val status: ReservationStatus = ReservationStatus.RESERVED
) : Parcelable

@Serializable
enum class GroupStatus {
    @SerialName("ACTIVE")
    ACTIVE,
    @SerialName("COMPLETED")
    COMPLETED,
    @SerialName("EXPIRED")
    EXPIRED,
    @SerialName("PICKUP")
    PICKUP,
    @SerialName("VALIDATED")
    VALIDATED
}

@Serializable
enum class ReservationStatus {
    @SerialName("RESERVED")
    RESERVED,
    @SerialName("VALIDATED")
    VALIDATED,
    @SerialName("EXPIRED")
    EXPIRED,
    @SerialName("CANCELLED")
    CANCELLED
}
