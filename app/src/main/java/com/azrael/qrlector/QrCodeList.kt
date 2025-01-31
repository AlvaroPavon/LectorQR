package com.azrael.qrlector

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import com.azrael.qrlector.network.QrApi
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun QrCodeList() {
    val qrApi = RetrofitInstance.api
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val response = qrApi.getQrCodes()
            if (response.isSuccessful) {
                qrCodes = response.body() ?: emptyList()
            } else {
                println("Error al obtener QR Codes: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column {
        qrCodes.forEach { qrCode ->
            Text(text = "Contenido: ${qrCode.contenido}, Descripción: ${qrCode.descripcion}")
        }
    }
}
