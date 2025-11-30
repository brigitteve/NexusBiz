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
     * - Letras, números y espacios (tildes incluidas)
     * - Máximo 60 caracteres
     */
    fun isValidName(value: String): Boolean {
        if (value.isBlank() || value.length > 60) return false
        return Regex("^[A-Za-z0-9ÁÉÍÓÚáéíóúñÑ ]+$").matches(value.trim())
    }
    
    /**
     * Sanitiza nombre (letras, números y espacios, máximo 60 caracteres)
     */
    fun sanitizeName(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9ÁÉÍÓÚáéíóúñÑ ]"), "").take(60)
    }

    /**
     * Valida alias de usuario (cliente o bodega)
     * - Letras, números y caracteres especiales comunes
     * - Sin espacios
     * - Máximo 30 caracteres
     */
    fun isValidAlias(value: String): Boolean {
        if (value.isBlank() || value.length > 30) return false
        // Permite letras, números y caracteres especiales comunes (sin espacios)
        return Regex("^[A-Za-z0-9._\\-!@#\$%^&*()+=\\[\\]{}|;:'\",.<>?/~`]+$").matches(value)
    }

    /**
     * Sanitiza alias permitiendo letras, números y caracteres especiales comunes
     */
    fun sanitizeAlias(value: String): String {
        // Permite letras, números y caracteres especiales comunes (sin espacios)
        return value.replace(Regex("[^A-Za-z0-9._\\-!@#\$%^&*()+=\\[\\]{}|;:'\",.<>?/~`]"), "").take(30)
    }
    
    /**
     * Valida contraseña
     * - Mínimo 6 caracteres
     * - Permite cualquier carácter (letras, números, caracteres especiales)
     */
    fun isValidPassword(value: String): Boolean {
        return value.length >= 6
    }
    
    /**
     * Valida dirección
     * - Letras, números, espacios, comas, puntos, guiones, numerales, barras, paréntesis, dos puntos, punto y coma
     * - Mínimo 5 caracteres
     * - Caracteres permitidos: A-Z, a-z, 0-9, espacios, ., ,, -, #, /, (, ), :, ;
     */
    fun isValidAddress(value: String): Boolean {
        if (value.isBlank() || value.length < 5) return false
        return Regex("^[A-Za-z0-9ÁÉÍÓÚáéíóúñÑ .,#/():;-]+$").matches(value.trim())
    }
    
    /**
     * Sanitiza dirección permitiendo caracteres comunes en direcciones
     * Permite: letras, números, espacios, puntos, comas, guiones, numerales (#), 
     * barras (/), paréntesis, dos puntos (:), punto y coma (;)
     */
    fun sanitizeAddress(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9ÁÉÍÓÚáéíóúñÑ .,#/():;-]"), "")
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
        const val INVALID_NAME = "Solo se permiten letras, números y espacios"
        const val INVALID_ALIAS = "Alias inválido (usa letras, números o caracteres especiales)"
        const val INVALID_ADDRESS = "Dirección inválida"
        const val INVALID_PRICE = "Precio inválido"
        const val INVALID_GROUP_PRICE = "El precio grupal debe ser menor al normal"
        const val INVALID_QUANTITY = "Cantidad inválida (mínimo 1)"
        const val INVALID_HOUR = "Horario inválido (formato HH:mm)"
        const val INVALID_TIME_RANGE = "La hora de inicio debe ser menor que la de fin"
    }
}

