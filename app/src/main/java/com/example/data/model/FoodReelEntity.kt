package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_reels")
data class FoodReelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customTitle: String,
    val originalTitle: String,
    val reelUrl: String,
    val creatorHandle: String,
    val cuisine: String,
    val category: String,
    val prepTime: String,
    val cookTime: String,
    val totalTime: String,
    val servings: String,
    val difficulty: String,
    val calories: String,
    val macros: String,
    val ingredientsJson: String,
    val instructionsJson: String,
    val chefTipsJson: String,
    val pairingSuggestion: String,
    val userNotes: String = "",
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val cookedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val accentColorHex: String = "#FF6D00"
)
