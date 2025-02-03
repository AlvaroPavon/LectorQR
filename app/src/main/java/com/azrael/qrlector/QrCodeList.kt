package com.azrael.qrlector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.azrael.qrlector.network.QrApi
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun QrCodeList() {
    val qrApi = RetrofitInstance.api
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var showAlert by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }
    var selectedContent by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedQrCode by remember { mutableStateOf<QrCode?>(null) }

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
            ClickableText(
                text = AnnotatedString(qrCode.contenido),
                onClick = {
                    selectedContent = qrCode.contenido
                    actionType = if (isValidUrl(qrCode.contenido)) "Abrir enlace" else "Copiar texto"
                    showAlert = true
                },
                style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Text(text = "Descripción: ${qrCode.descripcion}", fontSize = 16.sp)
            Button(onClick = {
                selectedQrCode = qrCode
                showEditDialog = true
            }) {
                Text("Modificar")
            }
        }
    }

    if (showAlert) {
        ConfirmActionDialog(
            title = "Confirmar Acción",
            message = "¿Quieres $actionType?",
            onConfirm = {
                showAlert = false
                handleQrCodeAction(context, selectedContent, clipboardManager)
            },
            onDismiss = { showAlert = false }
        )
    }

    if (showEditDialog && selectedQrCode != null) {
        EditQrCodeDialog(qrCode = selectedQrCode!!, onDismiss = { showEditDialog = false })
    }
}

fun handleQrCodeAction(context: Context, content: String, clipboardManager: ClipboardManager) {
    if (isValidUrl(content)) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
        context.startActivity(intent)
    } else {
        val clip = ClipData.newPlainText("QR Code", content)
        clipboardManager.setPrimaryClip(clip)
        println("Texto copiado al portapapeles: $content")
    }
}

fun isValidUrl(url: String): Boolean {
    return try {
        Uri.parse(url).scheme?.let { scheme ->
            scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
        } ?: false
    } catch (e: Exception) {
        false
    }
}

@Composable
fun QrCodeListDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("QR Escaneados Anteriores", style = MaterialTheme.typography.titleMedium) },
        text = { QrCodeList() },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cerrar")
            }
        }
    )
}
