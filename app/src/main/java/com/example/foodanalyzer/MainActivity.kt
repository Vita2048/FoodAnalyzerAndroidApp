package com.example.foodanalyzer
import android.app.Activity
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodanalyzer.ui.theme.FoodAnalyzerTheme
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModel
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.camera.view.PreviewView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodAnalyzerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodAdditiveScreen()
                }
            }
        }
    }
}

@Composable
fun FoodAdditiveScreen(
    viewModel: FoodAdditiveViewModel = viewModel(
        factory = FoodAdditiveViewModelFactory(LocalContext.current)
    )
) {
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    val analysisResult by viewModel.analysisResult.collectAsState()
    var analysisAttempted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Replace with your Google Cloud Translation API key
    val apiKey = "AIzaSyCgwqIjZ6S0SNlJ0LcJqaUiXjQf44JOCG0"

    // State for camera and OCR
    var showCamera by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            showCamera = true
        } else {
            Log.e("MainActivity", "Required permissions denied")
        }
    }

    val uCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "uCrop result: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                Log.d("MainActivity", "uCrop result URI: $resultUri")
                coroutineScope.launch {
                    isProcessingOcr = true
                    try {
                        val recognizedText = performOcr(context, resultUri)
                        inputText = recognizedText
                        Log.d("MainActivity", "Recognized text: $recognizedText")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "OCR failed", e)
                    } finally {
                        isProcessingOcr = false
                    }
                }
            } else {
                Log.e("MainActivity", "uCrop returned null URI")
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data!!)
            Log.e("MainActivity", "uCrop error: ${error?.message}", error)
        } else {
            Log.e("MainActivity", "uCrop failed with result code: ${result.resultCode}")
        }
    }

    // Check and request permissions
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { uri ->
                capturedImageUri = uri
                showCamera = false
                // Launch uCrop for cropping
                val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_image.jpg"))
                val uCrop = UCrop.of(uri, destinationUri)
                    .withAspectRatio(0f, 0f) // Free crop
                    .withMaxResultSize(1080, 1080)
                uCropLauncher.launch(uCrop.getIntent(context))
            },
            onCancel = { showCamera = false }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Food Additive Analyzer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            item {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter food additives (comma-separated)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTranslating = true
                                try {
                                    val translated = translateText(inputText, apiKey)
                                    translatedText = translated
                                    Log.d("MainActivity", "Translated text: $translatedText")
                                    viewModel.analyzeAdditives(translatedText)
                                    analysisAttempted = true
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Translation failed", e)
                                    viewModel.analyzeAdditives(inputText)
                                    analysisAttempted = true
                                } finally {
                                    isTranslating = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        enabled = !isTranslating
                    ) {
                        Text(if (isTranslating) "Translating..." else "Analyze")
                    }
                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamera = true
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        enabled = !isProcessingOcr
                    ) {
                        Text(if (isProcessingOcr) "Processing..." else "Scan")
                    }
                }
            }

            if (analysisAttempted) {
                if (analysisResult == null) {
                    item {
                        Text(
                            text = "No additives found in the ingredient list.",
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "Found Additives:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .background(Color.Blue)
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Code", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Name", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Severity", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(analysisResult!!) { additive ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(additive.code, color = Color.Blue, modifier = Modifier.weight(1f))
                            Text(additive.name, modifier = Modifier.weight(2f))
                            val context = LocalContext.current
                            val resourceId = context.resources.getIdentifier(
                                "vertical_gauge_${additive.severity}",
                                "drawable",
                                context.packageName
                            )
                            if (resourceId == 0) {
                                Log.e("MainActivity", "VectorDrawable not found for severity ${additive.severity}")
                                Text(
                                    text = "Error",
                                    color = Color.Red,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            } else {
                                Log.d("MainActivity", "VectorDrawable ID for severity ${additive.severity}: $resourceId")
                                Image(
                                    painter = painterResource(id = resourceId),
                                    contentDescription = "Severity ${additive.severity}",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                Text(
                    text = "Scale of Harmfulness of Food Additives",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .background(Color.Blue)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Severity", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Description", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                }
            }
            items(viewModel.harmfulnessScale) { scaleItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val resourceId = context.resources.getIdentifier(
                        "vertical_gauge_${scaleItem.severity}",
                        "drawable",
                        context.packageName
                    )
                    Box(
                        modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resourceId == 0) {
                            Log.e("MainActivity", "VectorDrawable not found for severity ${scaleItem.severity}")
                            Text(
                                text = "Error",
                                color = Color.Red
                            )
                        } else {
                            Log.d("MainActivity", "VectorDrawable ID for severity ${scaleItem.severity}: $resourceId")
                            Image(
                                painter = painterResource(id = resourceId),
                                contentDescription = "Severity ${scaleItem.severity}",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        text = scaleItem.description,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(3f).padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture = ImageCapture.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    Log.d("CameraScreen", "Camera bound successfully")
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera provider initialization failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    try {
                        Log.d("CameraScreen", "Capture button clicked")
                        val photoFile = createImageFile(context)
                        Log.d("CameraScreen", "Photo file created: ${photoFile.absolutePath}")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture?.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    try {
                                        Log.d("CameraScreen", "Image saved successfully")
                                        val savedUri = FileProvider.getUriForFile(
                                            context,
                                            "com.example.foodanalyzer.fileprovider",
                                            photoFile
                                        )
                                        Log.d("CameraScreen", "Saved URI: $savedUri")
                                        onImageCaptured(savedUri)
                                    } catch (e: Exception) {
                                        Log.e("CameraScreen", "Failed to process saved image", e)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Image capture failed: ${exception.message}", exception)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Capture button error", e)
                    }
                }
            ) {
                Text("Capture")
            }
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
private fun createImageFile(context: Context): File {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir == null || !storageDir.exists()) {
            Log.e("CameraScreen", "Storage directory is null or does not exist")
            throw IllegalStateException("Storage directory unavailable")
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            Log.d("CameraScreen", "Created file: ${absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to create image file", e)
        throw e
    }
}

suspend fun performOcr(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        source
    } else {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
    }

    val image = InputImage.fromBitmap(bitmap, 0)
    var recognizedText = ""

    try {
        val result = recognizer.process(image).await()
        recognizedText = result.text
    } catch (e: Exception) {
        Log.e("MainActivity", "OCR processing failed", e)
        throw e
    } finally {
        recognizer.close()
    }

    return@withContext recognizedText
}

// Extension to make ML Kit's Task awaitable
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = withContext(Dispatchers.IO) {
    com.google.android.gms.tasks.Tasks.await(this@await)
}

// Function to translate text using Google Cloud Translation API via HTTP
suspend fun translateText(text: String, apiKey: String): String = withContext(Dispatchers.IO) {
    if (text.isBlank()) return@withContext text

    try {
        val client = OkHttpClient()
        val requestBodyJson = JSONObject().apply {
            put("q", text)
            put("target", "en")
            put("format", "text")
        }.toString()

        val request = Request.Builder()
            .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
            .post(
                requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("MainActivity", "Translation API failed: ${response.code} - ${response.message}")
                throw Exception("Translation API failed: ${response.code} - ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            Log.d("MainActivity", "Translation API response: $responseBody")

            val jsonResponse = JSONObject(responseBody)
            val translations = jsonResponse
                .getJSONObject("data")
                .getJSONArray("translations")
            if (translations.length() > 0) {
                return@withContext translations.getJSONObject(0).getString("translatedText")
            } else {
                throw Exception("No translations found in response")
            }
        }
    } catch (e: Exception) {
        Log.e("MainActivity", "Translation error", e)
        throw e
    }
}
