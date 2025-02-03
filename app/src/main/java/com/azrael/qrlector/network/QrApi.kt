package com.azrael.qrlector.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class QrCode(
    val id: Long? = null,  // Hacer el id opcional
    var contenido: String,
    var descripcion: String,
    var modificaciones: Int = 0,
    var fechaModificacion: String? = null
)



interface QrApi {

    @GET("qr-codes")
    suspend fun getQrCodes(): Response<List<QrCode>>

    @POST("qr-codes")
    suspend fun saveQrCode(@Body qrCode: QrCode): Response<QrCode>

    @PUT("qr-codes/{id}")
    suspend fun updateQrCode(@Path("id") id: Long, @Body qrCode: QrCode): Response<QrCode>

    @DELETE("qr-codes/{id}")
    suspend fun deleteQrCode(@Path("id") id: Long): Response<Unit>
}