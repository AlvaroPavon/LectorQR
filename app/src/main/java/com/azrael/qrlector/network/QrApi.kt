package com.azrael.qrlector.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class QrCode(val id: Long? = null, val contenido: String, val descripcion: String)

interface QrApi {
    @GET("qr-codes")
    suspend fun getQrCodes(): Response<List<QrCode>>

    @POST("qr-codes")
    suspend fun saveQrCode(@Body qrCode: QrCode): Response<QrCode>
}
