package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(
    onNavigateToTable: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: BiegoveViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settingsState.collectAsState()
    val selectedRace by viewModel.selectedRaceObject.collectAsState()
    val useDraw = settings?.useDraw ?: false

    // Sheet State
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Biegove", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = selectedRace?.name ?: "No Race Selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedRace != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSheet = true }) {
                        Icon(painter = painterResource(R.drawable.list), contentDescription = "Select Race")
                    }
                    IconButton(onClick = onNavigateToTable) {
                        Icon(painter = painterResource(R.drawable.table), contentDescription = "Table")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(painter = painterResource(R.drawable.settings), contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (selectedRace == null) {
                // Display this when no race is active
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Please select or create a race",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    androidx.compose.material3.Button(
                        onClick = { showSheet = true },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Open Race Selector")
                    }
                }
            } else {
                // Content only visible if a race is selected
                if (useDraw) {
                    DrMainPage(viewModel)
                } else {
                    KbMainPage(PaddingValues(0.dp), snackbarHostState, viewModel)
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                RaceSelectorSheet(viewModel) { showSheet = false }
            }
        }
    }
}