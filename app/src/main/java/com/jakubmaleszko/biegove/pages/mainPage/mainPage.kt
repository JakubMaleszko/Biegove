package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    val useDraw = settings?.useDraw ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(title = { Text("Biegove", maxLines = 1) }, actions = {
                IconButton(onClick = onNavigateToTable) {
                    Icon(
                        painter = painterResource(R.drawable.table),
                        contentDescription = "Table view"
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings page"
                    )
                }
            })
        }
    ) { innerPadding ->
        if (useDraw) {
            DrMainPage(innerPadding, snackbarHostState, viewModel)
        } else {
            KbMainPage(innerPadding, snackbarHostState, viewModel)
        }
    }
}
