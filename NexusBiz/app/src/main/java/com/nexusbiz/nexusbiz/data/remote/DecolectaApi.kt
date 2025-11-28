package com.nexusbiz.nexusbiz.data.remote

import com.nexusbiz.nexusbiz.data.model.RucResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DecolectaApi {

    @GET("v1/sunat/ruc")
    suspend fun consultarRuc(
        @Query("numero") numero: String,
        @Header("Authorization") authorization: String =
            "Bearer sk_11956.tZokBQi0lsiR8nIVuAk283z8sILolcFG"
    ): RucResponse
}
