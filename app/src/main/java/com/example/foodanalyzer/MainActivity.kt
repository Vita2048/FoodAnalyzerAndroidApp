package com.example.foodanalyzer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.foundation.Image // Corrected import for Image composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Food Additive Analyzer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter food additives (comma-separated)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.analyzeAdditives(inputText) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Analyze")
        }

        when (analysisResult) {
            null -> {
                Text(
                    text = "No additives found in the ingredient list.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            else -> {
                Text(
                    text = "Found Additives:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn {
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
                }
            }
        }

        Text(
            text = "Scale of Harmfulness of Food Additives",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn {
            items(viewModel.harmfulnessScale) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(item.color)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${item.severity}: ${item.description}",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
