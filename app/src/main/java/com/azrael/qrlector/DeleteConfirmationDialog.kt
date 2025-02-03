package com.azrael.qrlector

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.azrael.qrlector.network.QrCode

@Composable
fun DeleteConfirmationDialog(
    qrCode: QrCode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Confirmar Eliminación") },
        text = {
            Column {
                Text("¿Estás seguro de que quieres eliminar este QR Code?")
                Text("Contenido: ${qrCode.contenido}")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}
