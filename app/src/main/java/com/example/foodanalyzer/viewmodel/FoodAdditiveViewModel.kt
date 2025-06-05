package com.example.foodanalyzer.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.foodanalyzer.model.Additive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

data class HarmfulnessLevel(
    val severity: String,
    val description: String,
    val color: Color
)

class FoodAdditiveViewModel(private val context: Context) : ViewModel() {
    private val _analysisResult = MutableStateFlow<List<Additive>?>(null) // Changed to List<Additive>
    val analysisResult: StateFlow<List<Additive>?> = _analysisResult

    val harmfulnessScale = listOf(
        HarmfulnessLevel("0", "Natural substance, obtained naturally", Color(0xFFCCFFCC)),
        HarmfulnessLevel("1", "Naturally occurring substance obtained synthetically", Color(0xFF99FF99)),
        HarmfulnessLevel("2", "Synthetic ingredient, no known side effects", Color(0xFF66FF66)),
        HarmfulnessLevel("3", "Ingredient not suitable for children, allergy sufferers, people sensitive to chemicals in food, ...", Color(0xFFFFF999)),
        HarmfulnessLevel("4", "Ingredient suspected of causing allergies, hyperactivity, ...", Color(0xFFFFCC66)),
        HarmfulnessLevel("5", "Ingredient likely to cause allergies, hyperactivity, ...", Color(0xFFFF9999)),
        HarmfulnessLevel("6", "Ingredient that may have carcinogenic effects", Color(0xFFFF6666))
    )

    private val additives: List<Additive> = loadAdditivesFromCsv()

    private fun loadAdditivesFromCsv(): List<Additive> {
        val additiveList = mutableListOf<Additive>()
        try {
            val inputStream = context.resources.openRawResource(
                context.resources.getIdentifier("additives", "raw", context.packageName)
            )
            val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8")))
            reader.readLine() // Skip header
            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= 3) {
                    val additive = Additive(
                        code = tokens[0].trim(),
                        name = tokens[1].trim(),
                        severity = tokens[2].trim()
                    )
                    additiveList.add(additive)
                }
            }
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            _analysisResult.value = null // Indicate error by setting to null
        }
        return additiveList
    }

    fun analyzeAdditives(input: String) {
        val ingredients = input.split("[,;:]".toRegex()).map { it.trim() } // Updated to match JavaScript
        if (ingredients.isEmpty() || ingredients.all { it.isEmpty() }) {
            _analysisResult.value = null // Indicate no results
            return
        }

        val foundAdditives = mutableMapOf<String, Additive>()
        for (ingredient in ingredients) {
            val cleanedIngredient = ingredient.replace(Regex("e\\s*(\\d{3}[a-z]?)"), { "e${it.groupValues[1]}" }).lowercase()
            var longestMatch: Additive? = null
            var longestMatchName = ""

            for (additive in additives) {
                val additiveCode = additive.code.lowercase()
                val additiveName = additive.name.lowercase()
                if (cleanedIngredient.contains(additiveName) || cleanedIngredient.contains(additiveCode)) {
                    if (additiveName.length > longestMatchName.length) {
                        longestMatch = additive
                        longestMatchName = additiveName
                    }
                }
            }

            if (longestMatch != null) {
                foundAdditives[longestMatch.code] = longestMatch
            }
        }

        _analysisResult.value = if (foundAdditives.isEmpty()) null else foundAdditives.values.toList()
    }
}
