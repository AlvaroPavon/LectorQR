package com.azrael.qrlector

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.azrael.qrlector.network.QrCode

@Composable
fun DeleteConfirmationDialog(
    qrCode: QrCode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.confirmar_eliminaci_n)) },
        text = {
            Column {
                Text(stringResource(R.string.est_s_seguro_de_que_quieres_eliminar_este_qr_code))
                Text(stringResource(R.string.contenido, qrCode.contenido))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text(stringResource(R.string.eliminar))
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}
