package com.example.data.repository

import com.example.data.local.FoodJsonConverter
import com.example.data.local.FoodReelDao
import com.example.data.model.FoodReelEntity
import com.example.data.model.IngredientItem
import com.example.data.remote.GeminiChefService
import kotlinx.coroutines.flow.Flow

class ReelChefRepository(
    private val dao: FoodReelDao
) {
    val allReels: Flow<List<FoodReelEntity>> = dao.getAllReels()
    val favoriteReels: Flow<List<FoodReelEntity>> = dao.getFavoriteReels()

    fun getReelById(id: Long): Flow<FoodReelEntity?> = dao.getReelById(id)

    fun searchReels(query: String): Flow<List<FoodReelEntity>> = dao.searchReels(query)

    fun getReelsByCategory(category: String): Flow<List<FoodReelEntity>> = dao.getReelsByCategory(category)

    suspend fun analyzeAndSaveReel(
        url: String,
        hint: String = "",
        customTitleOverride: String = ""
    ): Long {
        val analysis = GeminiChefService.analyzeFoodReel(url, hint)
        val finalTitle = if (customTitleOverride.isNotBlank()) customTitleOverride else analysis.dishName

        // Choose a vibrant accent color based on cuisine
        val colorHex = when (analysis.category.lowercase()) {
            "pasta" -> "#E65100" // Warm Paprika Orange
            "desserts" -> "#C2185B" // Berry Pink
            "quick meals" -> "#00897B" // Fresh Teal
            "street food" -> "#F57C00" // Sunset Amber
            "healthy" -> "#2E7D32" // Basil Green
            else -> "#D84315" // Chef Flame Red
        }

        val entity = FoodReelEntity(
            customTitle = finalTitle,
            originalTitle = analysis.dishName,
            reelUrl = url,
            creatorHandle = analysis.creatorHandle,
            cuisine = analysis.cuisine,
            category = analysis.category,
            prepTime = analysis.prepTime,
            cookTime = analysis.cookTime,
            totalTime = analysis.totalTime,
            servings = analysis.servings,
            difficulty = analysis.difficulty,
            calories = analysis.calories,
            macros = analysis.macros,
            ingredientsJson = FoodJsonConverter.ingredientsToJson(analysis.ingredients),
            instructionsJson = FoodJsonConverter.instructionsToJson(analysis.instructions),
            chefTipsJson = FoodJsonConverter.stringsToJson(analysis.chefTips),
            pairingSuggestion = analysis.pairingSuggestion,
            userNotes = if (hint.isNotBlank()) "Saved note: $hint" else "",
            rating = 5,
            isFavorite = false,
            cookedCount = 0,
            createdAt = System.currentTimeMillis(),
            accentColorHex = colorHex
        )

        return dao.insertReel(entity)
    }

    suspend fun renameReel(id: Long, newTitle: String) {
        if (newTitle.isNotBlank()) {
            dao.updateTitle(id, newTitle.trim())
        }
    }

    suspend fun toggleFavorite(id: Long, current: Boolean) {
        dao.updateFavorite(id, !current)
    }

    suspend fun updateNotesAndRating(id: Long, notes: String, rating: Int) {
        dao.updateNotesAndRating(id, notes, rating)
    }

    suspend fun toggleIngredientCheck(reelId: Long, currentIngredients: List<IngredientItem>, index: Int) {
        if (index in currentIngredients.indices) {
            val updated = currentIngredients.mapIndexed { idx, item ->
                if (idx == index) item.copy(isChecked = !item.isChecked) else item
            }
            dao.updateIngredientsJson(reelId, FoodJsonConverter.ingredientsToJson(updated))
        }
    }

    suspend fun markCooked(id: Long) {
        dao.incrementCookedCount(id)
    }

    suspend fun deleteReel(id: Long) {
        dao.deleteReelById(id)
    }

    suspend fun prepopulateSampleReelsIfEmpty() {
        if (dao.getCount() == 0) {
            val sample1Analysis = GeminiChefService.generateSmartFallback(
                "https://www.instagram.com/reel/C89xYzPqT12/",
                "Creamy Tuscan Garlic Butter Pasta"
            )
            val sample2Analysis = GeminiChefService.generateSmartFallback(
                "https://www.instagram.com/reel/C72aBvMxt99/",
                "Viral 10-Minute Garlic Chili Oil Ramen"
            )
            val sample3Analysis = GeminiChefService.generateSmartFallback(
                "https://www.instagram.com/reel/C9X1yKlM233/",
                "Crispy Cheesy Birria Smash Tacos"
            )
            val sample4Analysis = GeminiChefService.generateSmartFallback(
                "https://www.instagram.com/reel/C6Z7uVwN888/",
                "Molten Lava Chocolate Mug Cake"
            )

            val samples = listOf(
                FoodReelEntity(
                    customTitle = "Creamy Tuscan Garlic Pasta (Viral Reel)",
                    originalTitle = sample1Analysis.dishName,
                    reelUrl = "https://www.instagram.com/reel/C89xYzPqT12/",
                    creatorHandle = "@pastaking_official",
                    cuisine = sample1Analysis.cuisine,
                    category = "Pasta",
                    prepTime = sample1Analysis.prepTime,
                    cookTime = sample1Analysis.cookTime,
                    totalTime = sample1Analysis.totalTime,
                    servings = sample1Analysis.servings,
                    difficulty = sample1Analysis.difficulty,
                    calories = sample1Analysis.calories,
                    macros = sample1Analysis.macros,
                    ingredientsJson = FoodJsonConverter.ingredientsToJson(sample1Analysis.ingredients),
                    instructionsJson = FoodJsonConverter.instructionsToJson(sample1Analysis.instructions),
                    chefTipsJson = FoodJsonConverter.stringsToJson(sample1Analysis.chefTips),
                    pairingSuggestion = sample1Analysis.pairingSuggestion,
                    userNotes = "Tried this on Friday night! Added a pinch of nutmeg and it was legendary.",
                    rating = 5,
                    isFavorite = true,
                    cookedCount = 2,
                    createdAt = System.currentTimeMillis() - 3600000 * 24,
                    accentColorHex = "#E65100"
                ),
                FoodReelEntity(
                    customTitle = "Late Night 10-Min Chili Oil Ramen",
                    originalTitle = sample2Analysis.dishName,
                    reelUrl = "https://www.instagram.com/reel/C72aBvMxt99/",
                    creatorHandle = "@tokyo_streetchef",
                    cuisine = sample2Analysis.cuisine,
                    category = "Quick Meals",
                    prepTime = sample2Analysis.prepTime,
                    cookTime = sample2Analysis.cookTime,
                    totalTime = sample2Analysis.totalTime,
                    servings = sample2Analysis.servings,
                    difficulty = sample2Analysis.difficulty,
                    calories = sample2Analysis.calories,
                    macros = sample2Analysis.macros,
                    ingredientsJson = FoodJsonConverter.ingredientsToJson(sample2Analysis.ingredients),
                    instructionsJson = FoodJsonConverter.instructionsToJson(sample2Analysis.instructions),
                    chefTipsJson = FoodJsonConverter.stringsToJson(sample2Analysis.chefTips),
                    pairingSuggestion = sample2Analysis.pairingSuggestion,
                    userNotes = "Make sure the oil is smoking hot before pouring over the garlic!",
                    rating = 5,
                    isFavorite = true,
                    cookedCount = 1,
                    createdAt = System.currentTimeMillis() - 3600000 * 12,
                    accentColorHex = "#00897B"
                ),
                FoodReelEntity(
                    customTitle = "Crispy Birria Smash Tacos Hack",
                    originalTitle = sample3Analysis.dishName,
                    reelUrl = "https://www.instagram.com/reel/C9X1yKlM233/",
                    creatorHandle = "@tacos_del_fuego",
                    cuisine = sample3Analysis.cuisine,
                    category = "Street Food",
                    prepTime = sample3Analysis.prepTime,
                    cookTime = sample3Analysis.cookTime,
                    totalTime = sample3Analysis.totalTime,
                    servings = sample3Analysis.servings,
                    difficulty = sample3Analysis.difficulty,
                    calories = sample3Analysis.calories,
                    macros = sample3Analysis.macros,
                    ingredientsJson = FoodJsonConverter.ingredientsToJson(sample3Analysis.ingredients),
                    instructionsJson = FoodJsonConverter.instructionsToJson(sample3Analysis.instructions),
                    chefTipsJson = FoodJsonConverter.stringsToJson(sample3Analysis.chefTips),
                    pairingSuggestion = sample3Analysis.pairingSuggestion,
                    userNotes = "The cheese crust on the edge makes this 10/10.",
                    rating = 4,
                    isFavorite = false,
                    cookedCount = 0,
                    createdAt = System.currentTimeMillis() - 3600000 * 4,
                    accentColorHex = "#F57C00"
                ),
                FoodReelEntity(
                    customTitle = "2-Min Molten Lava Mug Cake",
                    originalTitle = sample4Analysis.dishName,
                    reelUrl = "https://www.instagram.com/reel/C6Z7uVwN888/",
                    creatorHandle = "@sweettooth_bakes",
                    cuisine = sample4Analysis.cuisine,
                    category = "Desserts",
                    prepTime = sample4Analysis.prepTime,
                    cookTime = sample4Analysis.cookTime,
                    totalTime = sample4Analysis.totalTime,
                    servings = sample4Analysis.servings,
                    difficulty = sample4Analysis.difficulty,
                    calories = sample4Analysis.calories,
                    macros = sample4Analysis.macros,
                    ingredientsJson = FoodJsonConverter.ingredientsToJson(sample4Analysis.ingredients),
                    instructionsJson = FoodJsonConverter.instructionsToJson(sample4Analysis.instructions),
                    chefTipsJson = FoodJsonConverter.stringsToJson(sample4Analysis.chefTips),
                    pairingSuggestion = sample4Analysis.pairingSuggestion,
                    userNotes = "Perfect 11 PM emergency dessert.",
                    rating = 5,
                    isFavorite = true,
                    cookedCount = 3,
                    createdAt = System.currentTimeMillis() - 3600000 * 2,
                    accentColorHex = "#C2185B"
                )
            )
            dao.insertAll(samples)
        }
    }
}
