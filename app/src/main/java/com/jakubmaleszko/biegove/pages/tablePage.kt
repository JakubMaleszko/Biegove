package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatUnixTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablePage(onBack: () -> Unit, viewModel: BiegoveViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val timestamps by viewModel.allTimestamps.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (timestamps.isEmpty()) {
                Text("No entries found", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        items = timestamps,
                        key = { it.uid } // Still keep the key for smooth list transitions
                    ) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Number Label
                                    Text(
                                        text = "#${item.number}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.3f)
                                    )

                                    // Date/Time
                                    Text(
                                        text = formatUnixTimestamp(item.timestamp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f)
                                    )

                                    // The Delete Button
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.removeTimestamp(item)

                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Item #${item.number} deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )

                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.addTimestamp(item.number)
                                                }
                                            }
                                        },

                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete),
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}