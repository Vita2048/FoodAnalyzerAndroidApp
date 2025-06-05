
package com.example.foodanalyzer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodanalyzer.ui.theme.FoodAnalyzerTheme
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModel
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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

    // Replace with your Google Cloud Translation API key
    val apiKey = "AIzaSyCgwqIjZ6S0SNlJ0LcJqaUiXjQf44JOCG0" // Replace with your actual API key

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
            Button(
                onClick = {
                    coroutineScope.launch {
                        isTranslating = true
                        try {
                            // Translate the input text to English
                            val translated = translateText(inputText, apiKey)
                            translatedText = translated
                            Log.d("MainActivity", "Translated text: $translatedText")
                            // Analyze the translated text
                            viewModel.analyzeAdditives(translatedText)
                            analysisAttempted = true
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Translation failed", e)
                            // Fallback: Analyze the original text if translation fails
                            viewModel.analyzeAdditives(inputText)
                            analysisAttempted = true
                        } finally {
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isTranslating
            ) {
                Text(if (isTranslating) "Translating..." else "Analyze")
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

// Function to translate text using Google Cloud Translation API via HTTP
suspend fun translateText(text: String, apiKey: String): String = withContext(Dispatchers.IO) {
    if (text.isBlank()) return@withContext text // Skip translation for empty text

    try {
        val client = OkHttpClient()

        // Build the JSON request body
        val requestBodyJson = JSONObject().apply {
            put("q", text)
            put("target", "en") // Target language: English
            put("format", "text")
        }.toString()

        // Create the HTTP request
        val request = Request.Builder()
            .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
            .post(
                requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        // Execute the request
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("MainActivity", "Translation API failed: ${response.code} - ${response.message}")
                throw Exception("Translation API failed: ${response.code} - ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            Log.d("MainActivity", "Translation API response: $responseBody")

            // Parse the JSON response
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
        throw e // Rethrow to handle in the calling coroutine
    }
}
