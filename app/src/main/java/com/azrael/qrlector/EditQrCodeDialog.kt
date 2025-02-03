package com.azrael.qrlector

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EditQrCodeDialog(qrCode: QrCode, onDismiss: () -> Unit) {
    var newContent by remember { mutableStateOf(TextFieldValue(qrCode.contenido)) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Modificar QR Code") },
        text = {
            Column {
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("Contenido del QR") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    qrCode.contenido = newContent.text
                    qrCode.modificaciones += 1
                    qrCode.fechaModificacion = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    qrCode.descripcion = "QR modificado (${qrCode.modificaciones} veces) ${qrCode.fechaModificacion}"
                    qrCode.id?.let { updateQrCode(context, it, qrCode) }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}

fun updateQrCode(context: Context, id: Long, qrCode: QrCode) {
    val qrApi = RetrofitInstance.api

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = qrApi.updateQrCode(id, qrCode)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println("QR Code actualizado exitosamente: ${response.body()}")
                } else {
                    println("Error al actualizar QR Code: ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
