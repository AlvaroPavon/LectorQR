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
import androidx.compose.ui.res.stringResource
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
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * @author Alvaro Pavon Martinez
 *
 * Clase principal de la aplicación que extiende AppCompatActivity.
 *
 * Esta actividad establece el contenido de la UI mediante la función composable [MyApp].
 */
class MainActivity : AppCompatActivity() {
    /**
     * Método que se ejecuta al crear la actividad.
     *
     * @param savedInstanceState Estado guardado de la actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

/**
 * Función composable principal que representa la interfaz de usuario de la aplicación.
 *
 * En ella se gestionan:
 * - La visualización del código QR generado (imagen y texto).
 * - El escaneo de nuevos códigos QR, solicitando permisos de cámara si es necesario.
 * - La persistencia del código QR en el servidor mediante Retrofit.
 * - La interacción del usuario a través de diálogos para confirmar acciones, listar códigos escaneados y eliminar códigos.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    // Contexto de la aplicación
    val context = LocalContext.current

    // Estado para gestionar el permiso de cámara
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Estado que almacena el contenido del código QR escaneado
    var qrCode by remember { mutableStateOf("") }

    // Estado que almacena el bitmap generado a partir del contenido del código QR
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Estados para controlar la visibilidad de diálogos y alertas
    var showDialog by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    // Lista de códigos QR previamente escaneados
    var qrCodes by remember { mutableStateOf<List<QrCode>>(emptyList()) }

    // Estados para la confirmación de eliminación y para almacenar el código QR a eliminar
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var qrCodeToDelete by remember { mutableStateOf<QrCode?>(null) }

    // Estado para ejecutar acciones (fuera del bloque composable)
    var executeAction by remember { mutableStateOf(false) }

    // Lanzador para iniciar el escaneo del código QR mediante [ScanContract]
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            qrCode = result.contents
            saveQrCode(context, qrCode)
            qrBitmap = generateQrCodeBitmap(qrCode)
        }
    }

    /**
     * Función local para actualizar un código QR en la lista de códigos almacenados.
     *
     * @param qrCode El código QR actualizado.
     */
    fun updateQrCodeLocal(qrCode: QrCode) {
        qrCodes = qrCodes.map {
            if (it.id == qrCode.id) {
                qrCode
            } else {
                it
            }
        }
    }

    // Estructura principal de la UI utilizando Scaffold
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
                // Muestra el bitmap del código QR, si ha sido generado
                qrBitmap?.let {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
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
                            .clickable { showAlert = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_code),
                            modifier = Modifier.size(250.dp)
                        )
                    }
                    // Muestra el contenido del código QR en forma de texto
                    Text(
                        text = qrCode,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Si el permiso de cámara está concedido, se muestra el botón para escanear el código QR
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
                    // Si no se tiene permiso de cámara, se muestra un mensaje y un botón para solicitarlo
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
                // Botón para mostrar los códigos QR escaneados anteriormente
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
                    Text(stringResource(R.string.qr_escaneados_anteriores), fontSize = 18.sp)
                }

                // Diálogo que muestra la lista de códigos QR escaneados anteriormente
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

                // Diálogo de confirmación para eliminar un código QR
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

                // Diálogo para confirmar la acción al pulsar sobre el código QR mostrado
                if (showAlert) {
                    ConfirmActionDialog(
                        title = stringResource(R.string.confirmar_acci_n),
                        message = stringResource(R.string.quieres, stringResource(R.string.abrir_enlace)),
                        onConfirm = {
                            executeAction = true  // Se activa el estado para ejecutar la acción
                            showAlert = false
                        },
                        onDismiss = { showAlert = false }
                    )
                }

                // Si se confirma la acción, se ejecuta la acción correspondiente fuera del bloque composable
                if (executeAction) {
                    handleQrCodeAction(context, qrCode)
                    executeAction = false
                }
            }
        }
    )
}

/**
 * Guarda el código QR escaneado en un servidor remoto utilizando Retrofit.
 *
 * @param context El contexto de la aplicación.
 * @param qrCode El contenido del código QR a guardar.
 */
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

/**
 * Maneja la acción asociada al contenido del código QR.
 *
 * Si el contenido es una URL válida, se abre en el navegador.
 * De lo contrario, se copia el contenido al portapapeles.
 *
 * @param context El contexto de la aplicación.
 * @param content El contenido del código QR.
 */
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

/**
 * Genera un Bitmap que representa el código QR a partir del contenido proporcionado.
 *
 * @param content El contenido a codificar en el código QR.
 * @return Un Bitmap con el código QR, o null si ocurre un error durante la generación.
 */
fun generateQrCodeBitmap(content: String): Bitmap? {
    val writer = QRCodeWriter()
    return try {
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val black = androidx.compose.ui.graphics.Color.Black.toArgb() // Color negro en formato Int
        val white = androidx.compose.ui.graphics.Color.White.toArgb() // Color blanco en formato Int
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

/**
 * Elimina un código QR previamente guardado en el servidor remoto utilizando su ID.
 *
 * @param context El contexto de la aplicación.
 * @param id El identificador único del código QR a eliminar.
 */
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
