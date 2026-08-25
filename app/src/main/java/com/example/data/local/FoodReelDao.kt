package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FoodReelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodReelDao {

    @Query("SELECT * FROM food_reels ORDER BY createdAt DESC")
    fun getAllReels(): Flow<List<FoodReelEntity>>

    @Query("SELECT * FROM food_reels WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteReels(): Flow<List<FoodReelEntity>>

    @Query("SELECT * FROM food_reels WHERE category = :category ORDER BY createdAt DESC")
    fun getReelsByCategory(category: String): Flow<List<FoodReelEntity>>

    @Query("SELECT * FROM food_reels WHERE id = :id LIMIT 1")
    fun getReelById(id: Long): Flow<FoodReelEntity?>

    @Query("SELECT * FROM food_reels WHERE customTitle LIKE '%' || :query || '%' OR originalTitle LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchReels(query: String): Flow<List<FoodReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: FoodReelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reels: List<FoodReelEntity>)

    @Update
    suspend fun updateReel(reel: FoodReelEntity)

    @Query("UPDATE food_reels SET customTitle = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)

    @Query("UPDATE food_reels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE food_reels SET userNotes = :notes, rating = :rating WHERE id = :id")
    suspend fun updateNotesAndRating(id: Long, notes: String, rating: Int)

    @Query("UPDATE food_reels SET cookedCount = cookedCount + 1 WHERE id = :id")
    suspend fun incrementCookedCount(id: Long)

    @Query("UPDATE food_reels SET ingredientsJson = :ingredientsJson WHERE id = :id")
    suspend fun updateIngredientsJson(id: Long, ingredientsJson: String)

    @Delete
    suspend fun deleteReel(reel: FoodReelEntity)

    @Query("DELETE FROM food_reels WHERE id = :id")
    suspend fun deleteReelById(id: Long)

    @Query("SELECT COUNT(*) FROM food_reels")
    suspend fun getCount(): Int
}
