package com.azrael.qrlector

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import com.azrael.qrlector.network.QrCode
import com.azrael.qrlector.network.RetrofitInstance
import com.google.accompanist.permissions.isGranted


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

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            qrCode = result.contents
            saveQrCode(qrCode)
        }
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

            if (qrCode.isNotEmpty()) {
                Text(text = "QR Code: $qrCode")
            }

            // Mostrar la lista de QR Codes guardados
            QrCodeList()
        }
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


