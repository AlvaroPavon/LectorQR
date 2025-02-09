package com.azrael.qrlector.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Data class que representa un código QR.
 *
 * @property id Identificador único del código QR (opcional).
 * @property contenido Contenido del código QR.
 * @property descripcion Descripción asociada al código QR.
 * @property fechaModificacion Fecha de la última modificación (opcional).
 */
data class QrCode(
    val id: Long? = null,  // Hacer el id opcional
    var contenido: String,
    var descripcion: String,
    var fechaModificacion: String? = null,
)

/**
 * Interfaz que define los endpoints para interactuar con la API REST de códigos QR.
 */
interface QrApi {

    /**
     * Recupera la lista de códigos QR almacenados en el servidor.
     *
     * @return Una [Response] que contiene una lista de objetos [QrCode].
     */
    @GET("qr-codes")
    suspend fun getQrCodes(): Response<List<QrCode>>

    /**
     * Guarda un nuevo código QR en el servidor.
     *
     * @param qrCode Objeto [QrCode] que se desea guardar.
     * @return Una [Response] que contiene el código QR guardado.
     */
    @POST("qr-codes")
    suspend fun saveQrCode(@Body qrCode: QrCode): Response<QrCode>

    /**
     * Actualiza un código QR existente en el servidor.
     *
     * @param id Identificador único del código QR a actualizar.
     * @param qrCode Objeto [QrCode] con los datos actualizados.
     * @return Una [Response] que contiene el código QR actualizado.
     */
    @PUT("qr-codes/{id}")
    suspend fun updateQrCode(@Path("id") id: Long, @Body qrCode: QrCode): Response<QrCode>

    /**
     * Elimina un código QR existente del servidor.
     *
     * @param id Identificador único del código QR a eliminar.
     * @return Una [Response] que indica el resultado de la operación de eliminación.
     */
    @DELETE("qr-codes/{id}")
    suspend fun deleteQrCode(@Path("id") id: Long): Response<Unit>
}
