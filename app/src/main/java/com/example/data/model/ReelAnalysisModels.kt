package com.example.data.model

data class IngredientItem(
    val name: String,
    val amount: String = "",
    var isChecked: Boolean = false
)

data class CookingStep(
    val stepNumber: Int,
    val instruction: String,
    val timerSeconds: Int? = null, // e.g. 300 for 5 mins
    val tip: String? = null
)

data class ReelAnalysisResult(
    val dishName: String,
    val cuisine: String,
    val category: String,
    val prepTime: String,
    val cookTime: String,
    val totalTime: String,
    val servings: String,
    val difficulty: String,
    val calories: String,
    val macros: String,
    val ingredients: List<IngredientItem>,
    val instructions: List<CookingStep>,
    val chefTips: List<String>,
    val pairingSuggestion: String,
    val creatorHandle: String = "@foodlover"
)
