package com.azrael.qrlector

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Muestra un diálogo de confirmación de acción.
 *
 * Esta función composable despliega un [AlertDialog] que solicita al usuario confirmar o cancelar
 * una acción determinada. Se muestran un título y un mensaje, y se proporcionan botones para confirmar o cancelar.
 *
 * @param title Título que se mostrará en el diálogo.
 * @param message Mensaje que se mostrará en el cuerpo del diálogo.
 * @param onConfirm Función lambda que se ejecuta cuando el usuario confirma la acción.
 * @param onDismiss Función lambda que se ejecuta cuando el usuario descarta o cierra el diálogo.
 */
@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(stringResource(R.string.s))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        }
    )
}
