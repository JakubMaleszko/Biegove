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

    // State for the Edit Dialog
    var editingResult by remember { mutableStateOf<Timestamp?>(null) }

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

    // --- Edit Dialog Logic ---
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
                        // Inside TablePage LazyColumn items
                        items(items = displayedResults, key = { it.id }) { result ->
                            ResultItem(
                                result = result,
                                onEdit = { editingResult = result },
                                onDelete = {
                                    scope.launch {
                                        // Perform deletion
                                        viewModel.removeTimestamp(result)

                                        // Show snackbar from the Page scope, not the Item scope
                                        val snackResult = snackbarHostState.showSnackbar(
                                            message = "Runner #${result.number} removed",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )

                                        if (snackResult == SnackbarResult.ActionPerformed) {
                                            // Re-insert the exact object
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.isEmpty() || filtered.toInt() < 60) {
                                seconds = filtered
                            }
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
                val newNumber = number.toIntOrNull() ?: result.number
                onConfirm(result.copy(number = newNumber, time = totalSecs))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResultItem(
    result: Timestamp,
    onDelete: () -> Unit, // Change this
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
){
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#${result.number}", modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(result.time), modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodyLarge)

                // Edit Button
                IconButton(onClick = onEdit) {
                    Icon(painterResource(R.drawable.edit), "Edit", tint = MaterialTheme.colorScheme.primary)
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(painterResource(R.drawable.delete), "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}