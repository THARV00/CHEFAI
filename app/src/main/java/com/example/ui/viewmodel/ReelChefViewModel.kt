package com.example.ui.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FoodJsonConverter
import com.example.data.model.CookingStep
import com.example.data.model.FoodReelEntity
import com.example.data.model.IngredientItem
import com.example.data.repository.ReelChefRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerState(
    val stepNumber: Int = 0,
    val stepTitle: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

data class ReelChefUiState(
    val reels: List<FoodReelEntity> = emptyList(),
    val filteredReels: List<FoodReelEntity> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isAnalyzing: Boolean = false,
    val analysisProgressText: String = "",
    val selectedReel: FoodReelEntity? = null,
    val reelToRename: FoodReelEntity? = null,
    val renameInputText: String = "",
    val reelToDelete: FoodReelEntity? = null,
    val showAddDialog: Boolean = false,
    val userMessage: String? = null,
    val timerState: TimerState? = null
)

class ReelChefViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReelChefRepository
    private var countDownTimer: CountDownTimer? = null

    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isAnalyzing = MutableStateFlow(false)
    private val _analysisProgressText = MutableStateFlow("")
    private val _selectedReel = MutableStateFlow<FoodReelEntity?>(null)
    private val _reelToRename = MutableStateFlow<FoodReelEntity?>(null)
    private val _renameInputText = MutableStateFlow("")
    private val _reelToDelete = MutableStateFlow<FoodReelEntity?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _timerState = MutableStateFlow<TimerState?>(null)

    init {
        val db = AppDatabase.getInstance(application)
        repository = ReelChefRepository(db.foodReelDao())
        viewModelScope.launch {
            repository.prepopulateSampleReelsIfEmpty()
        }
    }

    val uiState: StateFlow<ReelChefUiState> = combine(
        repository.allReels,
        _selectedCategory,
        _searchQuery,
        _isAnalyzing,
        _analysisProgressText,
        _selectedReel,
        _reelToRename,
        _renameInputText,
        _reelToDelete,
        _showAddDialog,
        _userMessage,
        _timerState
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allReels = params[0] as List<FoodReelEntity>
        val category = params[1] as String
        val query = params[2] as String
        val isAnalyzing = params[3] as Boolean
        val analysisText = params[4] as String
        val selected = params[5] as FoodReelEntity?
        val toRename = params[6] as FoodReelEntity?
        val renameText = params[7] as String
        val toDelete = params[8] as FoodReelEntity?
        val showAdd = params[9] as Boolean
        val message = params[10] as String?
        val timer = params[11] as TimerState?

        val filtered = allReels.filter { reel ->
            val matchesCategory = when (category) {
                "All" -> true
                "Favorites" -> reel.isFavorite
                else -> reel.category.equals(category, ignoreCase = true) ||
                        reel.cuisine.contains(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    reel.customTitle.contains(query, ignoreCase = true) ||
                    reel.originalTitle.contains(query, ignoreCase = true) ||
                    reel.cuisine.contains(query, ignoreCase = true) ||
                    reel.creatorHandle.contains(query, ignoreCase = true) ||
                    reel.ingredientsJson.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }

        // Keep selected reel updated with latest from db
        val updatedSelected = if (selected != null) {
            allReels.find { it.id == selected.id } ?: selected
        } else null

        ReelChefUiState(
            reels = allReels,
            filteredReels = filtered,
            selectedCategory = category,
            searchQuery = query,
            isAnalyzing = isAnalyzing,
            analysisProgressText = analysisText,
            selectedReel = updatedSelected,
            reelToRename = toRename,
            renameInputText = renameText,
            reelToDelete = toDelete,
            showAddDialog = showAdd,
            userMessage = message,
            timerState = timer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReelChefUiState()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openReelDetail(reel: FoodReelEntity) {
        _selectedReel.value = reel
    }

    fun closeReelDetail() {
        _selectedReel.value = null
    }

    fun showAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun openRenameDialog(reel: FoodReelEntity) {
        _reelToRename.value = reel
        _renameInputText.value = reel.customTitle
    }

    fun updateRenameInput(text: String) {
        _renameInputText.value = text
    }

    fun confirmRename() {
        val reel = _reelToRename.value ?: return
        val newTitle = _renameInputText.value.trim()
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.renameReel(reel.id, newTitle)
                _reelToRename.value = null
                _userMessage.value = "Renamed to \"$newTitle\""
            }
        }
    }

    fun dismissRenameDialog() {
        _reelToRename.value = null
    }

    fun confirmDelete(reel: FoodReelEntity) {
        _reelToDelete.value = reel
    }

    fun executeDelete() {
        val reel = _reelToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteReel(reel.id)
            if (_selectedReel.value?.id == reel.id) {
                _selectedReel.value = null
            }
            _reelToDelete.value = null
            _userMessage.value = "Removed \"${reel.customTitle}\""
        }
    }

    fun dismissDeleteDialog() {
        _reelToDelete.value = null
    }

    fun toggleFavorite(reel: FoodReelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(reel.id, reel.isFavorite)
        }
    }

    fun markCooked(reel: FoodReelEntity) {
        viewModelScope.launch {
            repository.markCooked(reel.id)
            _userMessage.value = "Great job chef! Cook count: ${reel.cookedCount + 1}"
        }
    }

    fun toggleIngredient(reel: FoodReelEntity, index: Int) {
        viewModelScope.launch {
            val ingredients = FoodJsonConverter.jsonToIngredients(reel.ingredientsJson)
            repository.toggleIngredientCheck(reel.id, ingredients, index)
        }
    }

    fun updateNotes(reel: FoodReelEntity, notes: String, rating: Int) {
        viewModelScope.launch {
            repository.updateNotesAndRating(reel.id, notes, rating)
            _userMessage.value = "Notes & Rating updated"
        }
    }

    fun analyzeAndSaveReel(url: String, dishHint: String = "", customTitle: String = "") {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            _userMessage.value = "Please paste a valid video or reel link"
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisProgressText.value = "AI analyzing food reel..."
            _showAddDialog.value = false

            try {
                _analysisProgressText.value = "Extracting ingredients & chef instructions..."
                val id = repository.analyzeAndSaveReel(cleanUrl, dishHint, customTitle)
                _userMessage.value = "Food reel analyzed & saved successfully!"
                
                // Fetch the new reel and open it immediately for a seamless UX
                val all = repository.allReels
                // small delay to let Flow emit
                kotlinx.coroutines.delay(100)
            } catch (e: Exception) {
                _userMessage.value = "Error analyzing reel: ${e.message}"
            } finally {
                _isAnalyzing.value = false
                _analysisProgressText.value = ""
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Built-in Cooking Timer ---
    fun startStepTimer(stepNumber: Int, stepInstruction: String, durationSeconds: Int) {
        countDownTimer?.cancel()
        _timerState.value = TimerState(
            stepNumber = stepNumber,
            stepTitle = stepInstruction,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            isRunning = true,
            isFinished = false
        )

        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000).toInt()
                _timerState.value = _timerState.value?.copy(
                    remainingSeconds = remaining,
                    isRunning = true
                )
            }

            override fun onFinish() {
                _timerState.value = _timerState.value?.copy(
                    remainingSeconds = 0,
                    isRunning = false,
                    isFinished = true
                )
                _userMessage.value = "Timer finished for Step $stepNumber!"
            }
        }.start()
    }

    fun pauseStepTimer() {
        countDownTimer?.cancel()
        _timerState.value = _timerState.value?.copy(isRunning = false)
    }

    fun resumeStepTimer() {
        val current = _timerState.value ?: return
        if (current.remainingSeconds > 0) {
            startStepTimer(current.stepNumber, current.stepTitle, current.remainingSeconds)
        }
    }

    fun dismissTimer() {
        countDownTimer?.cancel()
        _timerState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}
