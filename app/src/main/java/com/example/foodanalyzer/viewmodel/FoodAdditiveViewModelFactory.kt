package com.example.foodanalyzer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FoodAdditiveViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodAdditiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoodAdditiveViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}