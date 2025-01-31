package com.azrael.qrlector

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.asImageBitmap
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.azrael.qrlector.ConfirmActionDialog

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
    val context = LocalContext.current
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()

    // Establecer el color de la barra de notificaciones
    SideEffect {
        val window = (context as AppCompatActivity).window
        window.statusBarColor = if (isDarkTheme) {
            androidx.compose.ui.graphics.Color.Black.toArgb() // colorPrimary para tema oscuro
        } else {
            androidx.compose.ui.graphics.Color(0xFF6200EE).toArgb() // colorPrimary para tema claro
        }
        ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !isDarkTheme
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var qrCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            qrCode = result.contents
            saveQrCode(qrCode)
            qrBitmap = generateQrCodeBitmap(qrCode)
        }
    }

    val onQrCodeClick: () -> Unit = {
        showAlert = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Scanner App", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                qrBitmap?.let {
                    Box(
                        modifier = Modifier
                            .size(300.dp) // Hacer el QR más grande
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .clickable(onClick = onQrCodeClick)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(250.dp)
                        )
                    }
                    Text(
                        text = qrCode,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (cameraPermissionState.status.isGranted) {
                    Button(
                        onClick = { scanLauncher.launch(ScanOptions()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Scan QR Code", fontSize = 18.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera permission is required to scan QR codes.", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Request Permission", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text("Ver QR Escaneados Anteriores", fontSize = 18.sp)
                }

                if (showDialog) {
                    QrCodeListDialog(onDismiss = { showDialog = false })
                }

                if (showAlert) {
                    ConfirmActionDialog(
                        title = "Confirmar Acción",
                        message = "¿Quieres abrir el enlace o copiar el texto?",
                        onConfirm = {
                            showAlert = false
                            handleQrCodeAction(context, qrCode)
                        },
                        onDismiss = { showAlert = false }
                    )
                }
            }
        }
    )
}

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

fun saveQrCode(qrCode: String) {
    val qrApi = RetrofitInstance.api
    val newQrCode = QrCode(contenido = qrCode, descripcion = "QR Code escaneado")

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = qrApi.saveQrCode(newQrCode)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println("QR Code guardado exitosamente: ${response.body()}")
                } else {
                    println("Error al guardar QR Code: ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun generateQrCodeBitmap(content: String): Bitmap? {
    val writer = QRCodeWriter()
    return try {
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val black = androidx.compose.ui.graphics.Color.Black.toArgb() // Convertir a Int
        val white = androidx.compose.ui.graphics.Color.White.toArgb() // Convertir a Int
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) black else white)
            }
        }
        bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

