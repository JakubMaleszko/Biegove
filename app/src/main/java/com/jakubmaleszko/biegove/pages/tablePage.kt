package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.R
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SortType { NUMBER, TIME }
enum class SortOrder { ASC, DESC }
fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablePage(onBack: () -> Unit, viewModel: BiegoveViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val results by viewModel.currentRaceResults.collectAsState()
    val selectedRace by viewModel.selectedRaceObject.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf(SortType.TIME) }
    var sortOrder by remember { mutableStateOf(SortOrder.DESC) }

    val displayedResults = remember(results, searchQuery, sortType, sortOrder) {
        results
            .filter { it.number.toString().contains(searchQuery) }
            .sortedWith { a, b ->
                val comparison = when (sortType) {
                    SortType.NUMBER -> a.number.compareTo(b.number)
                    SortType.TIME -> a.time.compareTo(b.time)
                }
                if (sortOrder == SortOrder.ASC) comparison else -comparison
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Race Results")
                        selectedRace?.let {
                            Text(it.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { input -> searchQuery = input.filter { it.isDigit() } },
                    label = { Text("Search by Number") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painter = painterResource(R.drawable.search), contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortType == SortType.NUMBER,
                        onClick = {
                            if (sortType == SortType.NUMBER) {
                                sortOrder = if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                            } else {
                                sortType = SortType.NUMBER
                                sortOrder = SortOrder.ASC
                            }
                        },
                        label = { Text("Number ${if (sortType == SortType.NUMBER) (if (sortOrder == SortOrder.ASC) "↑" else "↓") else ""}") }
                    )

                    FilterChip(
                        selected = sortType == SortType.TIME,
                        onClick = {
                            if (sortType == SortType.TIME) {
                                sortOrder = if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                            } else {
                                sortType = SortType.TIME
                                sortOrder = SortOrder.DESC
                            }
                        },
                        label = { Text("Time ${if (sortType == SortType.TIME) (if (sortOrder == SortOrder.ASC) "↑" else "↓") else ""}") }
                    )
                }
            }

            HorizontalDivider()

            Box(modifier = Modifier.weight(1f)) {
                if (displayedResults.isEmpty()) {
                    Text(
                        text = if (results.isEmpty()) "No runners recorded yet" else "No matches found",
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = displayedResults,
                            key = { it.id }
                        ) { result ->
                            ResultItem(
                                result = result,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                scope = scope,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultItem(
    result: Timestamp,
    viewModel: BiegoveViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#${result.number}", modifier = Modifier.weight(0.3f))
                Text(formatDuration(result.time), modifier = Modifier.weight(0.6f))
                IconButton(onClick = {
                    scope.launch {
                        viewModel.removeTimestamp(result)

                        val snackResult = snackbarHostState.showSnackbar(
                            message = "Runner #${result.number} removed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )

                        if (snackResult == SnackbarResult.ActionPerformed) {
                            // Ensure your ViewModel has a method to re-insert the specific object
                            viewModel.insertResultToSelectedRace(result)
                        }
                    }
                }) {
                    Icon(painterResource(R.drawable.delete), "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}