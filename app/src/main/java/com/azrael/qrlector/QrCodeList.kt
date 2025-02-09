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
import androidx.compose.ui.res.stringResource
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

/**
 * Muestra una lista de códigos QR obtenidos del servidor y permite realizar acciones sobre ellos.
 *
 * Esta función composable se encarga de:
 * - Obtener la lista de códigos QR a través de la API (Retrofit).
 * - Mostrar la lista en una LazyColumn con el contenido del QR.
 * - Permitir al usuario:
 *   - Seleccionar un código QR para abrir su enlace o copiar su contenido.
 *   - Editar un código QR mediante un diálogo de edición.
 *   - Eliminar un código QR mediante un diálogo de confirmación.
 *
 * Además, gestiona la actualización local de la lista y la ejecución de acciones fuera del bloque composable.
 *
 * @param onDismiss Función lambda que se invoca para descartar la lista o el diálogo.
 * @param onDelete Función lambda que se invoca al eliminar un código QR.
 * @param onEdit Función lambda que se invoca al editar un código QR.
 */
@Composable
fun QrCodeList(
    onDismiss: () -> Unit,
    onDelete: (QrCode) -> Unit,
    onEdit: (QrCode) -> Unit
) {
    // Obtención de la API configurada en Retrofit
    val qrApi = RetrofitInstance.api

    // Estado que almacena la lista de códigos QR obtenidos del servidor
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }

    // Contexto de la aplicación actual
    val context = LocalContext.current

    // Acceso al servicio del portapapeles para copiar contenido si es necesario
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Estados para controlar la visualización de diálogos y la selección de acciones
    var showAlert by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }
    var selectedContent by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedQrCode by remember { mutableStateOf<QrCode?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var qrCodeToDelete by remember { mutableStateOf<QrCode?>(null) }
    var executeAction by remember { mutableStateOf(false) }  // Estado para ejecutar la acción fuera de @Composable

    // Estado actualizado de la lista para evitar problemas de recomposición
    val updatedQrCodes by rememberUpdatedState(qrCodes)

    // Efecto que se lanza al inicio para obtener los códigos QR desde la API
    LaunchedEffect(Unit) {
        try {
            val response = qrApi.getQrCodes()
            if (response.isSuccessful) {
                qrCodes = response.body() ?: emptyList()
            } else {
                println(
                    context.getString(
                        R.string.error_al_obtener_qr_codes,
                        response.errorBody()?.string()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Actualiza localmente un código QR en la lista.
     *
     * @param qrCode El código QR actualizado.
     */
    fun updateLocalQrCode(qrCode: QrCode) {
        qrCodes = updatedQrCodes.map {
            if (it.id == qrCode.id) {
                qrCode
            } else {
                it
            }
        }
    }

    /**
     * Elimina localmente un código QR de la lista y en el servidor.
     *
     * @param id El identificador del código QR a eliminar.
     */
    fun deleteQrCode(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = qrApi.deleteQrCode(id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Elimina el código QR de la lista localmente
                        qrCodes = updatedQrCodes.filter { it.id != id }
                        println(context.getString(R.string.qr_code_eliminado_exitosamente))
                    } else {
                        println(
                            context.getString(
                                R.string.error_al_eliminar_qr_code,
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

    // Muestra la lista de códigos QR utilizando LazyColumn
    LazyColumn {
        items(updatedQrCodes) { qrCode ->
            Column {
                // Texto clicable que muestra el contenido del código QR
                // Al hacer clic, se prepara la acción: abrir enlace o copiar texto
                ClickableText(
                    text = AnnotatedString(qrCode.contenido),
                    onClick = {
                        selectedContent = qrCode.contenido
                        actionType = if (isValidUrl(qrCode.contenido))
                            context.getString(R.string.abrir_enlace)
                        else
                            context.getString(R.string.copiar_texto)
                        showAlert = true
                    },
                    style = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
                // Muestra el contenido del QR con un formato definido en los recursos
                Text(
                    stringResource(R.string.contenido_del_qr, qrCode.contenido),
                    fontSize = 16.sp
                )
                Row {
                    // Botón para modificar el código QR
                    Button(onClick = {
                        selectedQrCode = qrCode
                        showEditDialog = true
                    }) {
                        Text(stringResource(R.string.modificar))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Botón para eliminar el código QR
                    Button(onClick = {
                        qrCodeToDelete = qrCode
                        showDeleteConfirmation = true
                    }) {
                        Text(stringResource(R.string.eliminar))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Diálogo de confirmación para la acción (abrir enlace o copiar texto)
    if (showAlert) {
        ConfirmActionDialog(
            title = stringResource(R.string.confirmar_acci_n),
            message = stringResource(R.string.quieres, actionType),
            onConfirm = {
                executeAction = true  // Se activa el estado para ejecutar la acción
                showAlert = false
            },
            onDismiss = { showAlert = false }
        )
    }

    // Diálogo para editar el código QR seleccionado
    if (showEditDialog && selectedQrCode != null) {
        EditQrCodeDialog(
            qrCode = selectedQrCode!!,
            onDismiss = { showEditDialog = false },
            onEdit = { qrCode ->
                updateLocalQrCode(qrCode)
            }
        )
    }

    // Diálogo de confirmación para eliminar el código QR seleccionado
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

    // Ejecuta la acción (abrir enlace o copiar texto) fuera del bloque composable
    if (executeAction) {
        handleQrCodeAction(context, selectedContent)
        executeAction = false
    }
}
