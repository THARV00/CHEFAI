package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DetailRecipeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ReelChefViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReelChefViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.userMessage) {
                    uiState.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = uiState.selectedReel,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { selectedReel ->
                            if (selectedReel != null) {
                                BackHandler {
                                    viewModel.closeReelDetail()
                                }
                                DetailRecipeScreen(
                                    reel = selectedReel,
                                    onBack = { viewModel.closeReelDetail() },
                                    onRename = { viewModel.openRenameDialog(selectedReel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(selectedReel) },
                                    onDelete = { viewModel.confirmDelete(selectedReel) },
                                    onToggleIngredient = { index ->
                                        viewModel.toggleIngredient(selectedReel, index)
                                    },
                                    onMarkCooked = { viewModel.markCooked(selectedReel) },
                                    onUpdateNotes = { notes, rating ->
                                        viewModel.updateNotes(selectedReel, notes, rating)
                                    },
                                    onStartTimer = { stepNum, title, seconds ->
                                        viewModel.startStepTimer(stepNum, title, seconds)
                                    }
                                )
                            } else {
                                HomeScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
