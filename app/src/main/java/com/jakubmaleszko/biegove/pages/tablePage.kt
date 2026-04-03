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
import androidx.compose.ui.text.font.FontWeight
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
    var sortOrder by remember { mutableStateOf(SortOrder.ASC) }

    var editingResult by remember { mutableStateOf<Timestamp?>(null) }

    // 1. Calculate Rank (Ordinal Number) based on finish time (Oldest to Newest)
    val finishPositions = remember(results) {
        results.asSequence() // Use sequence for better performance on large lists
            .sortedBy { it.time }
            .mapIndexed { index, timestamp -> timestamp.id to (index + 1) }
            .toMap()
    }

    // Filtered results for display
    val displayedResults = remember(results, searchQuery, sortType, sortOrder) {
        results.filter { it.number.toString().contains(searchQuery) }
            .sortedWith { a, b ->
                val comp = if (sortType == SortType.NUMBER) a.number.compareTo(b.number)
                else a.time.compareTo(b.time)
                if (sortOrder == SortOrder.ASC) comp else -comp
            }
    }

    // Dialog handling
    editingResult?.let { result ->
        EditResultDialog(
            result = result,
            onDismiss = { editingResult = null },
            onConfirm = { updatedResult ->
                viewModel.updateTimestamp(updatedResult)
                editingResult = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Race Results")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            selectedRace?.let {
                                Text(it.name, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = " • ${results.size} entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // Search and Filter Chips
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { input -> searchQuery = input.filter { it.isDigit() } },
                    label = { Text("Search by Number") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painterResource(R.drawable.search), null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sortType == SortType.NUMBER,
                        onClick = {
                            if (sortType == SortType.NUMBER) sortOrder = if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                            else { sortType = SortType.NUMBER; sortOrder = SortOrder.ASC }
                        },
                        label = { Text("Number ${if (sortType == SortType.NUMBER) (if (sortOrder == SortOrder.ASC) "↑" else "↓") else ""}") }
                    )
                    FilterChip(
                        selected = sortType == SortType.TIME,
                        onClick = {
                            if (sortType == SortType.TIME) sortOrder = if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                            else { sortType = SortType.TIME; sortOrder = SortOrder.ASC }
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
                        items(items = displayedResults, key = { it.id }) { result ->
                            ResultItem(
                                result = result,
                                ordinal = finishPositions[result.id] ?: 0,
                                onEdit = { editingResult = result },
                                onDelete = {
                                    scope.launch {
                                        viewModel.removeTimestamp(result)
                                        val snackResult = snackbarHostState.showSnackbar(
                                            message = "Runner #${result.number} removed",
                                            actionLabel = "Undo"
                                        )
                                        if (snackResult == SnackbarResult.ActionPerformed) {
                                            viewModel.insertResultToSelectedRace(result)
                                        }
                                    }
                                },
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
fun EditResultDialog(
    result: Timestamp,
    onDismiss: () -> Unit,
    onConfirm: (Timestamp) -> Unit
) {
    var number by remember { mutableStateOf(result.number.toString()) }
    var minutes by remember { mutableStateOf((result.time / 60).toString()) }
    var seconds by remember { mutableStateOf(String.format("%02d", result.time % 60)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Result") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() } },
                    label = { Text("Runner Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { it.isDigit() } },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.isEmpty() || filtered.toInt() < 60) seconds = filtered
                        },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val totalSecs = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                onConfirm(result.copy(number = number.toIntOrNull() ?: result.number, time = totalSecs))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ResultItem(
    result: Timestamp,
    ordinal: Int,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ordinal.",
                    modifier = Modifier.width(35.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "#${result.number}",
                    modifier = Modifier.weight(0.3f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(result.time),
                    modifier = Modifier.weight(0.4f),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = onEdit) { Icon(painterResource(R.drawable.edit), null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}