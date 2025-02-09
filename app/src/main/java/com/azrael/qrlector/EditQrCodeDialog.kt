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

/**
 * Muestra un diálogo para editar un código QR.
 *
 * Este diálogo permite al usuario modificar el contenido de un código QR. Se muestra un campo de texto
 * con el contenido actual, y al confirmar se actualiza la fecha de modificación y la descripción del código QR.
 * Luego, se llama a [updateQrCode] para enviar la actualización al servidor y se notifica la actualización
 * mediante el callback [onEdit].
 *
 * @param qrCode Objeto [QrCode] que se desea editar.
 * @param onDismiss Función lambda que se invoca cuando se descarta el diálogo sin realizar cambios.
 * @param onEdit Función lambda que se invoca al confirmar la edición, pasando el objeto [QrCode] actualizado.
 */
@Composable
fun EditQrCodeDialog(qrCode: QrCode, onDismiss: () -> Unit, onEdit: (QrCode) -> Unit) {
    // Estado para el nuevo contenido del código QR
    var newContent by remember { mutableStateOf(TextFieldValue(qrCode.contenido)) }
    // Estado para la fecha de modificación; si no existe, se inicializa como cadena vacía
    var fechaModificacion by remember { mutableStateOf(qrCode.fechaModificacion ?: "") }
    // Estado para la descripción; si no existe, se inicializa como cadena vacía
    var descripcion by remember { mutableStateOf(qrCode.descripcion ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.modificar_qr_code)) },
        text = {
            Column {
                // Campo de texto para editar el contenido del código QR
                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text(stringResource(R.string.contenido_del_qr)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Se muestra la descripción actual
                Text(stringResource(R.string.descripci_n, descripcion))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()

                    // Actualiza la fecha de modificación usando el formato definido en los recursos
                    fechaModificacion = SimpleDateFormat(
                        context.getString(R.string.yyyy_mm_dd_hh_mm_ss),
                        Locale.getDefault()
                    ).format(Date())

                    // Actualiza la descripción con la nueva fecha de modificación
                    descripcion = context.getString(
                        R.string.qr_modificado_ltima_modificaci_n,
                        fechaModificacion
                    )

                    // Crea una copia actualizada del objeto QrCode
                    val updatedQrCode = qrCode.copy(
                        contenido = newContent.text,
                        fechaModificacion = fechaModificacion,
                        descripcion = descripcion
                    )

                    // Actualiza el código QR en el servidor
                    updateQrCode(context, updatedQrCode.id!!, updatedQrCode)
                    // Notifica la actualización a través del callback onEdit
                    onEdit(updatedQrCode)
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

/**
 * Actualiza un código QR en el servidor.
 *
 * Esta función realiza una llamada a la API utilizando Retrofit para actualizar un código QR en el servidor.
 * La operación se ejecuta en un hilo de fondo utilizando coroutines y, una vez completada la respuesta,
 * se notifica el resultado en el hilo principal.
 *
 * @param context Contexto de la aplicación utilizado para acceder a los recursos y mostrar mensajes.
 * @param id Identificador único del código QR que se desea actualizar.
 * @param qrCode Objeto [QrCode] con los datos actualizados.
 */
fun updateQrCode(context: Context, id: Long, qrCode: QrCode) {
    val qrApi = RetrofitInstance.api

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Realiza la petición para actualizar el código QR en el servidor
            val response = qrApi.updateQrCode(id, qrCode)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println(
                        context.getString(
                            R.string.qr_code_actualizado_exitosamente,
                            response.body()
                        )
                    )
                } else {
                    println(
                        context.getString(
                            R.string.error_al_actualizar_qr_code,
                            response.errorBody()?.string()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
