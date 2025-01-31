package com.azrael.qrlector

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

// Actividad principal
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    // Contexto y estados
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var qrCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    // Lanzador de actividad para escanear QR
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            qrCode = result.contents
            saveQrCode(qrCode)
            qrBitmap = generateQrCodeBitmap(qrCode)
        }
    }

    // Acción al hacer clic en el QR
    val onQrCodeClick: () -> Unit = {
        showAlert = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("QR Scanner App") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Comprobación de permisos de la cámara
            if (cameraPermissionState.status.isGranted) {
                Button(onClick = { scanLauncher.launch(ScanOptions()) }) {
                    Text("Scan QR Code")
                }
            } else {
                Column {
                    Text("Camera permission is required to scan QR codes.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Request Permission")
                    }
                }
            }

            // Mostrar la imagen del QR escaneado
            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.clickable(onClick = onQrCodeClick)
                )
            }

            // Botón para mostrar el diálogo de QR escaneados anteriormente
            Button(onClick = { showDialog = true }) {
                Text("Ver QR Escaneados Anteriores")
            }

            // Mostrar el diálogo si `showDialog` es verdadero
            if (showDialog) {
                QrCodeListDialog(onDismiss = { showDialog = false })
            }

            // Mostrar la alerta si `showAlert` es verdadero
            if (showAlert) {
                ConfirmActionDialog(
                    qrCode = qrCode,
                    onConfirm = {
                        showAlert = false
                        handleQrCodeAction(context, qrCode)
                    },
                    onDismiss = { showAlert = false }
                )
            }
        }
    }
}

// Función para manejar la acción según el contenido del QR
fun handleQrCodeAction(context: Context, content: String) {
    if (isValidUrl(content)) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
        context.startActivity(intent)
    } else {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code", content)
        clipboardManager.setPrimaryClip(clip)
        println("Texto copiado al portapapeles: $content")
    }
}

// Función para guardar el QR escaneado en la API
fun saveQrCode(qrCode: String) {
    val qrApi = RetrofitInstance.api
    val newQrCode = QrCode(contenido = qrCode, descripcion = "QR Code escaneado")

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = qrApi.saveQrCode(newQrCode)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    // Manejar éxito
                    println("QR Code guardado exitosamente: ${response.body()}")
                } else {
                    // Manejar error
                    println("Error al guardar QR Code: ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Función para generar la imagen del QR Code a partir del contenido escaneado
fun generateQrCodeBitmap(content: String): Bitmap? {
    val writer = QRCodeWriter()
    return try {
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

// Diálogo para mostrar la lista de QR escaneados anteriormente
@Composable
fun QrCodeListDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("QR Escaneados Anteriores") },
        text = { QrCodeList() },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cerrar")
            }
        }
    )
}

// Diálogo para confirmar la acción a realizar con el QR
@Composable
fun ConfirmActionDialog(
    qrCode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Confirmar Acción") },
        text = { Text("¿Quieres abrir el enlace o copiar el texto?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}
