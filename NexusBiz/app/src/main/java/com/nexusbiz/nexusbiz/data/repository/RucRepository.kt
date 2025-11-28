package com.nexusbiz.nexusbiz.data.repository

import com.nexusbiz.nexusbiz.data.model.RucResponse
import com.nexusbiz.nexusbiz.data.remote.DecolectaClient
import retrofit2.HttpException
import java.io.IOException

class RucRepository {

    private val api = DecolectaClient.api

    suspend fun consultarRuc(ruc: String): Result<RucResponse> {
        return try {
            val response = api.consultarRuc(ruc)
            Result.success(response)
        } catch (e: HttpException) {
            Result.failure(Exception("Error HTTP ${e.code()}: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error inesperado: ${e.message}"))
        }
    }
}


