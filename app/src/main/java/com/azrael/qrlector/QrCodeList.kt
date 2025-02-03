package com.azrael.qrlector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil.isValidUrl
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azrael.qrlector.network.QrApi
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun QrCodeList(
    onDismiss: () -> Unit,
    onDelete: (QrCode) -> Unit,
    onEdit: (QrCode) -> Unit
) {
    val qrApi = RetrofitInstance.api
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var showAlert by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }
    var selectedContent by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedQrCode by remember { mutableStateOf<QrCode?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var qrCodeToDelete by remember { mutableStateOf<QrCode?>(null) }

    val updatedQrCodes by rememberUpdatedState(qrCodes)

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

    fun updateLocalQrCode(qrCode: QrCode) {
        qrCodes = updatedQrCodes.map {
            if (it.id == qrCode.id) {
                qrCode
            } else {
                it
            }
        }
    }

    fun deleteQrCode(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = qrApi.deleteQrCode(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        qrCodes = updatedQrCodes.filter { it.id != id } // Eliminar localmente el QR Code de la lista
                        println("QR Code eliminado exitosamente")
                    } else {
                        println("Error al eliminar QR Code: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn {
        items(updatedQrCodes) { qrCode ->
            Column {
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
                Row {
                    Button(onClick = {
                        selectedQrCode = qrCode
                        showEditDialog = true
                    }) {
                        Text("Modificar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        qrCodeToDelete = qrCode
                        showDeleteConfirmation = true
                    }) {
                        Text("Eliminar")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showAlert) {
        ConfirmActionDialog(
            title = "Confirmar Acción",
            message = "¿Quieres $actionType?",
            onConfirm = {
                showAlert = false
                handleQrCodeAction(context, selectedContent)
            },
            onDismiss = { showAlert = false }
        )
    }

    if (showEditDialog && selectedQrCode != null) {
        EditQrCodeDialog(
            qrCode = selectedQrCode!!,
            onDismiss = { showEditDialog = false },
            onEdit = { qrCode ->
                updateLocalQrCode(qrCode)
            }
        )
    }

    if (showDeleteConfirmation && qrCodeToDelete != null) {
        DeleteConfirmationDialog(
            qrCode = qrCodeToDelete!!,
            onConfirm = {
                qrCodeToDelete!!.id?.let { deleteQrCode(it) }
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}







