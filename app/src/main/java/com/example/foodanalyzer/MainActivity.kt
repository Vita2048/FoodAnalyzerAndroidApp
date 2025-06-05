package com.example.foodanalyzer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Ensure this is the one being used
import androidx.compose.foundation.lazy.items // Ensure this is the one being used
// Remove unused scroll state imports if they were added before
// import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // For centering text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodanalyzer.ui.theme.FoodAnalyzerTheme
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModel
import com.example.foodanalyzer.viewmodel.FoodAdditiveViewModelFactory

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
    val analysisResult by viewModel.analysisResult.collectAsState()
    var analysisAttempted by remember { mutableStateOf(false) }

    LazyColumn( // Replaced Column with LazyColumn for a single scrollable page
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // Affects how items are aligned if not fillMaxWidth
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
                    viewModel.analyzeAdditives(inputText)
                    analysisAttempted = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Analyze")
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
                // Header for results table
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
                // Items for results table
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
                item { // Add some padding after the results table
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        item {
            Text(
                text = "Scale of Harmfulness of Food Additives",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp) // Ensure consistent padding
            )
        }
        // Header for scale table
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
        // Items for scale table
        items(viewModel.harmfulnessScale) { scaleItem -> // Renamed to avoid conflict
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