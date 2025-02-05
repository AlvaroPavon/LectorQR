package com.azrael.qrlector

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil.isValidUrl
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

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
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var qrCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var qrCodeToDelete by remember { mutableStateOf<QrCode?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            qrCode = result.contents
            saveQrCode(context,qrCode)
            qrBitmap = generateQrCodeBitmap(qrCode)
        }
    }

    val onQrCodeClick: () -> Unit = {
        showAlert = true
    }

    fun updateQrCodeLocal(qrCode: QrCode) {
        qrCodes = qrCodes.map {
            if (it.id == qrCode.id) {
                qrCode
            } else {
                it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_scanner_app), style = MaterialTheme.typography.titleLarge) },
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
                            contentDescription = stringResource(R.string.qr_code),
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
                        Text(stringResource(R.string.scan_qr_code), fontSize = 18.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.permiso_de_camara_es_requerido_para_escanear_qr_codes), fontSize = 16.sp)
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
                            Text(stringResource(R.string.request_permission), fontSize = 16.sp)
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
                    Text(stringResource(R.string.ver_qr_escaneados_anteriores), fontSize = 18.sp)
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text(stringResource(R.string.qr_escaneados_anteriores)) },
                        text = {
                            QrCodeList(
                                onDismiss = { showDialog = false },
                                onDelete = { qrCode ->
                                    qrCodeToDelete = qrCode
                                    showDeleteConfirmation = true
                                },
                                onEdit = { qrCode ->
                                    updateQrCodeLocal(qrCode)
                                }
                            )
                        },
                        confirmButton = {
                            Button(onClick = { showDialog = false }) {
                                Text(stringResource(R.string.cerrar))
                            }
                        }
                    )
                }

                if (showDeleteConfirmation) {
                    DeleteConfirmationDialog(
                        qrCode = qrCodeToDelete!!,
                        onConfirm = {
                            qrCodeToDelete!!.id?.let { deleteQrCode(context, it) }
                            showDeleteConfirmation = false
                        },
                        onDismiss = { showDeleteConfirmation = false }
                    )
                }

                if (showAlert) {
                    ConfirmActionDialog(
                        title = stringResource(R.string.confirmar_acci_n),
                        message = stringResource(R.string.quieres_abrir_el_enlace_o_copiar_el_texto),
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



@Composable
fun handleQrCodeAction(context: Context, content: String) {
    if (isValidUrl(content)) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
        context.startActivity(intent)
    } else {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(stringResource(R.string.qr_code), content)
        clipboardManager.setPrimaryClip(clip)
        println(stringResource(R.string.texto_copiado_al_portapapeles, content))
    }
}

fun saveQrCode(context: Context, qrCode: String) {
    val qrApi = RetrofitInstance.api
    val newQrCode = QrCode(contenido = qrCode, descripcion = context.getString(R.string.qr_code_escaneado))

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = qrApi.saveQrCode(newQrCode)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println(response.body()
                        ?.let { context.getString(R.string.qr_code_guardado_exitosamente, it) })
                } else {
                    println(response.errorBody()?.string()
                        ?.let { context.getString(R.string.error_al_guardar_qr_code, it) })
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
fun deleteQrCode(context: Context, id: Long) {
    val qrApi = RetrofitInstance.api

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = qrApi.deleteQrCode(id)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println(context.getString(R.string.qr_code_eliminado_exitosamente))
                } else {
                    println(
                        context.getString(
                            R.string.error_al_eliminar_qr_code,
                            response.errorBody()?.string()
                        ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

