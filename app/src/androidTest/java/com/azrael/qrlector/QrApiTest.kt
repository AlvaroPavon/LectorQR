package com.azrael.qrlector


import com.azrael.qrlector.network.QrApi
import com.azrael.qrlector.network.QrCode
import retrofit2.Response

/**
 * Test implementation of QrApi for unit testing
 */
private class TestQrApi : QrApi {
    private val testQrCode = QrCode(
        id = 1,
        contenido = "https://example.com",
        descripcion = "Test QR Code",
        fechaModificacion = "2024-02-09"
    )

    override suspend fun getQrCodes(): Response<List<QrCode>> {
        return Response.success(listOf(testQrCode))
    }

    override suspend fun saveQrCode(qrCode: QrCode): Response<QrCode> {
        return Response.success(qrCode.copy(id = 2))
    }

    override suspend fun updateQrCode(id: Long, qrCode: QrCode): Response<QrCode> {
        return Response.success(qrCode)
    }

    override suspend fun deleteQrCode(id: Long): Response<Unit> {
        return Response.success(Unit)
    }
}