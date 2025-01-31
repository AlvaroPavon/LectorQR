package com.azrael.qrlector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.azrael.qrlector.network.QrApi
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun QrCodeList() {
    // Obtener la instancia de la API y el contexto
    val qrApi = RetrofitInstance.api
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Cargar la lista de QR escaneados
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

    // Mostrar la lista de QR escaneados
    Column {
        qrCodes.forEach { qrCode ->
            ClickableText(
                text = AnnotatedString(qrCode.contenido),
                onClick = {
                    if (isValidUrl(qrCode.contenido)) {
                        // Abrir enlace en el navegador
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCode.contenido))
                        context.startActivity(intent)
                    } else {
                        // Copiar texto al portapapeles
                        val clip = ClipData.newPlainText("QR Code", qrCode.contenido)
                        clipboardManager.setPrimaryClip(clip)
                        println("Texto copiado al portapapeles: ${qrCode.contenido}")
                    }
                }
            )
            Text(text = "Descripción: ${qrCode.descripcion}")
        }
    }
}

// Función para verificar si una cadena es una URL válida
fun isValidUrl(url: String): Boolean {
    return try {
        Uri.parse(url).scheme?.let { scheme ->
            scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
        } ?: false
    } catch (e: Exception) {
        false
    }
}
