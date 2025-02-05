package com.azrael.qrlector

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
fun EditQrCodeDialog(qrCode: QrCode, onDismiss: () -> Unit, onEdit: (QrCode) -> Unit) {
    var newContent by remember { mutableStateOf(TextFieldValue(qrCode.contenido)) }
    var fechaModificacion by remember { mutableStateOf(qrCode.fechaModificacion ?: "") }
    var descripcion by remember { mutableStateOf(qrCode.descripcion ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.modificar_qr_code)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text(stringResource(R.string.contenido_del_qr)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.descripci_n, descripcion))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()

                    fechaModificacion = SimpleDateFormat(context.getString(R.string.yyyy_mm_dd_hh_mm_ss), Locale.getDefault()).format(Date())
                    descripcion = context.getString(
                        R.string.qr_modificado_ltima_modificaci_n,
                        fechaModificacion
                    )

                    val updatedQrCode = qrCode.copy(
                        contenido = newContent.text,
                        fechaModificacion = fechaModificacion,
                        descripcion = descripcion
                    )

                    updateQrCode(context, updatedQrCode.id!!, updatedQrCode)
                    onEdit(updatedQrCode) // Llamar a la función con el objeto actualizado
                }
            ) {
                Text(stringResource(R.string.guardar))
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancelar))
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
                    println(
                        context.getString(
                            R.string.qr_code_actualizado_exitosamente,
                            response.body()
                        ))
                } else {
                    println(
                        context.getString(
                            R.string.error_al_actualizar_qr_code,
                            response.errorBody()?.string()
                        ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
