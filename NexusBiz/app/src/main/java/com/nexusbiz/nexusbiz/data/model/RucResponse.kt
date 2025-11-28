package com.nexusbiz.nexusbiz.data.model

import com.google.gson.annotations.SerializedName

data class RucResponse(
    @SerializedName("razon_social")
    val razonSocial: String?,
    @SerializedName("numero_documento")
    val numeroDocumento: String?,
    @SerializedName("estado")
    val estado: String?,
    @SerializedName("condicion")
    val condicion: String?,
    @SerializedName("direccion")
    val direccion: String?,
    @SerializedName("ubigeo")
    val ubigeo: String?,
    @SerializedName("via_tipo")
    val viaTipo: String?,
    @SerializedName("via_nombre")
    val viaNombre: String?,
    @SerializedName("zona_codigo")
    val zonaCodigo: String?,
    @SerializedName("zona_tipo")
    val zonaTipo: String?,
    @SerializedName("numero")
    val numero: String?,
    @SerializedName("interior")
    val interior: String?,
    @SerializedName("lote")
    val lote: String?,
    @SerializedName("dpto")
    val dpto: String?,
    @SerializedName("manzana")
    val manzana: String?,
    @SerializedName("kilometro")
    val kilometro: String?,
    @SerializedName("distrito")
    val distrito: String?,
    @SerializedName("provincia")
    val provincia: String?,
    @SerializedName("departamento")
    val departamento: String?,
    @SerializedName("es_agente_retencion")
    val esAgenteRetencion: Boolean?,
    @SerializedName("es_buen_contribuyente")
    val esBuenContribuyente: Boolean?,
    @SerializedName("locales_anexos")
    val localesAnexos: List<String>?,
    @SerializedName("nombre_comercial")
    val nombreComercial: String?
)

