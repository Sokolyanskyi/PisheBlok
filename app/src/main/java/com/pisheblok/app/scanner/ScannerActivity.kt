package com.pisheblok.app.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pisheblok.app.R
import com.pisheblok.app.ui.theme.PisheBlokTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Full-screen barcode scanner. Returns RESULT_OK with "barcode" + optional
 * "productName" (looked up in the free Open Food Facts database).
 */
class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Intent()
        setContent {
            PisheBlokTheme {
                ScannerScreen(
                    onResult = { barcode, productName ->
                        result.putExtra("barcode", barcode)
                        result.putExtra("productName", productName ?: "")
                        setResult(RESULT_OK, result)
                        finish()
                    },
                    onClose = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ScannerScreen(onResult: (String, String?) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraScanner(onBarcode = onResult)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.scan_permission),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun CameraScanner(onBarcode: (String, String?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var searching by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        var done = false
        analysis.setAnalyzer(executor) { proxy ->
            if (done) {
                proxy.close()
                return@setAnalyzer
            }
            val image = proxy.image
            if (image == null) {
                proxy.close()
                return@setAnalyzer
            }
            val media = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
            scanner.process(media)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue
                    if (raw != null && !done) {
                        done = true
                        searching = true
                        scope.launch {
                            val name = lookupProduct(raw)
                            onBarcode(raw, name)
                        }
                    }
                }
                .addOnCompleteListener { proxy.close() }
        }
        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        } catch (_: Exception) {
            // camera may be unavailable; user can close the screen
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(300.dp, 180.dp)
                .border(3.dp, Color.White, RoundedCornerShape(24.dp))
        )
        if (searching) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC000000),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 110.dp),
            ) {
                Text(
                    stringResource(R.string.scan_searching),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                }) {
                    Icon(
                        if (torchOn) Icons.Rounded.FlashlightOff else Icons.Rounded.FlashlightOn,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    stringResource(R.string.scan_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Looks the barcode up in the free open databases (Open Food Facts, then
 * Open Products Facts for non-food goods). Ukrainian name is preferred,
 * then Russian, then the generic one. Returns "Name · Brand" or null.
 */
private suspend fun lookupProduct(barcode: String): String? =
    lookupInDatabase("https://world.openfoodfacts.org", barcode)
        ?: lookupInDatabase("https://world.openproductsfacts.org", barcode)

private suspend fun lookupInDatabase(host: String, barcode: String): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("$host/api/v2/product/$barcode.json?fields=product_name,product_name_ru,product_name_ua,brands")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(body)
        if (obj.optString("status") == "0") return@withContext null
        val product = obj.getJSONObject("product")
        val name = product.optString("product_name_ua")
            .ifBlank { product.optString("product_name_ru") }
            .ifBlank { product.optString("product_name") }
        val brands = product.optString("brands")
        listOf(name, brands)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { null }
    } catch (_: Exception) {
        null
    }
}
