package com.nexusbiz.nexusbiz.util

object Validators {
    /**
     * Valida número de teléfono peruano
     * - Solo números
     * - Debe iniciar con 9
     * - 9 dígitos exactos
     */
    fun isValidPhone(value: String): Boolean {
        val cleaned = value.replace(Regex("[^0-9]"), "")
        return Regex("^9\\d{8}$").matches(cleaned)
    }
    
    /**
     * Sanitiza número de teléfono (solo números, máximo 9 dígitos)
     */
    fun sanitizePhone(value: String): String {
        return value.replace(Regex("[^0-9]"), "").take(9)
    }
    
    /**
     * Valida nombres (persona, producto, bodega)
     * - Permite todos los caracteres
     * - Máximo 60 caracteres
     */
    fun isValidName(value: String): Boolean {
        if (value.isBlank() || value.length > 60) return false
        return true // Permite todos los caracteres
    }
    
    /**
     * Sanitiza nombre (permite todos los caracteres, máximo 60 caracteres)
     */
    fun sanitizeName(value: String): String {
        return value.take(60) // Solo limita la longitud, permite todos los caracteres
    }

    /**
     * Valida alias de usuario (cliente o bodega)
     * - Permite todos los caracteres
     * - Máximo 30 caracteres
     */
    fun isValidAlias(value: String): Boolean {
        if (value.isBlank() || value.length > 30) return false
        return true // Permite todos los caracteres
    }

    /**
     * Sanitiza alias (permite todos los caracteres, máximo 30 caracteres)
     */
    fun sanitizeAlias(value: String): String {
        return value.take(30) // Solo limita la longitud, permite todos los caracteres
    }
    
    /**
     * Valida dirección
     * - Permite todos los caracteres
     * - Mínimo 5 caracteres
     */
    fun isValidAddress(value: String): Boolean {
        if (value.isBlank() || value.length < 5) return false
        return true // Permite todos los caracteres
    }
    
    /**
     * Sanitiza dirección (permite todos los caracteres)
     */
    fun sanitizeAddress(value: String): String {
        return value // Permite todos los caracteres sin sanitizar
    }
    
    /**
     * Valida si es un número (permite decimales)
     */
    fun isNumber(value: String): Boolean {
        if (value.isBlank()) return false
        return Regex("^\\d*\\.?\\d+$").matches(value.trim())
    }
    
    /**
     * Valida si es un entero positivo
     */
    fun isInteger(value: String): Boolean {
        if (value.isBlank()) return false
        return Regex("^\\d+$").matches(value.trim())
    }
    
    /**
     * Valida precio (número positivo, puede ser decimal)
     */
    fun isValidPrice(value: String): Boolean {
        if (!isNumber(value)) return false
        val price = value.toDoubleOrNull() ?: return false
        return price > 0
    }
    
    /**
     * Valida que el precio grupal sea menor al precio normal
     */
    fun isValidGroupPrice(groupPrice: Double, normalPrice: Double): Boolean {
        return groupPrice > 0 && normalPrice > 0 && groupPrice < normalPrice
    }
    
    /**
     * Valida cantidad/target (entero positivo, mínimo 1)
     */
    fun isValidQuantity(value: String): Boolean {
        if (!isInteger(value)) return false
        val quantity = value.toIntOrNull() ?: return false
        return quantity >= 1
    }
    
    /**
     * Valida horario formato HH:mm
     */
    fun isValidHour(value: String): Boolean {
        return Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value.trim())
    }
    
    /**
     * Valida que hora de inicio sea menor que hora de fin
     */
    fun isValidTimeRange(startHour: String, endHour: String): Boolean {
        if (!isValidHour(startHour) || !isValidHour(endHour)) return false
        val start = startHour.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        val end = endHour.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        return start < end
    }
    
    /**
     * Mensajes de error estándar
     */
    object ErrorMessages {
        const val INVALID_PHONE = "Número de teléfono inválido"
        const val INVALID_NAME = "Nombre inválido (máximo 60 caracteres)"
        const val INVALID_ALIAS = "Alias inválido (máximo 30 caracteres)"
        const val INVALID_ADDRESS = "Dirección inválida (mínimo 5 caracteres)"
        const val INVALID_PRICE = "Precio inválido"
        const val INVALID_GROUP_PRICE = "El precio grupal debe ser menor al normal"
        const val INVALID_QUANTITY = "Cantidad inválida (mínimo 1)"
        const val INVALID_HOUR = "Horario inválido (formato HH:mm)"
        const val INVALID_TIME_RANGE = "La hora de inicio debe ser menor que la de fin"
    }
}

