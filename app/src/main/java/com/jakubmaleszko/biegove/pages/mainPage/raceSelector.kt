package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.R
import java.time.*
import java.time.format.DateTimeFormatter

// Updated to include seconds :ss
fun formatUnixTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceSelectorSheet(viewModel: BiegoveViewModel, onDismiss: () -> Unit) {
    val races by viewModel.allRaces.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var showAddSection by remember { mutableStateOf(false) }
    var editingRace by remember { mutableStateOf<com.jakubmaleszko.biegove.db.entities.Race?>(null) }

    val currentTime = remember { LocalTime.now() }
    var newRaceName by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var hour by remember { mutableStateOf(String.format("%02d", currentTime.hour)) }
    var minute by remember { mutableStateOf(String.format("%02d", currentTime.minute)) }
    var second by remember { mutableStateOf(String.format("%02d", currentTime.second)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var raceToDelete by remember { mutableStateOf<com.jakubmaleszko.biegove.db.entities.Race?>(null) }

    // --- DIALOGS SECTION ---

    // 1. Delete Confirmation Dialog
    if (raceToDelete != null) {
        AlertDialog(
            onDismissRequest = { raceToDelete = null },
            title = { Text("Delete Race?") },
            text = { Text("Are you sure you want to delete '${raceToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        raceToDelete?.let { viewModel.removeRace(it) }
                        raceToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { raceToDelete = null }) { Text("Cancel") } }
        )
    }

    // 2. Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- FORM HELPERS ---

    val resetForm = {
        val now = LocalDateTime.now()
        newRaceName = ""
        selectedDate = now.toLocalDate()
        hour = String.format("%02d", now.hour)
        minute = String.format("%02d", now.minute)
        second = String.format("%02d", now.second)
        showAddSection = false
        editingRace = null
    }

    val startEditing = { race: com.jakubmaleszko.biegove.db.entities.Race ->
        val dt = Instant.ofEpochMilli(race.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        editingRace = race
        newRaceName = race.name
        selectedDate = dt.toLocalDate()
        hour = String.format("%02d", dt.hour)
        minute = String.format("%02d", dt.minute)
        second = String.format("%02d", dt.second)
        showAddSection = true
    }

    // --- UI LAYOUT ---

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (editingRace != null) "Edit Race" else "Your Races",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = { if (showAddSection) resetForm() else showAddSection = true }) {
                Text(if (showAddSection) "Cancel" else "Add New")
            }
        }

        AnimatedVisibility(visible = showAddSection, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newRaceName,
                        onValueChange = { newRaceName = it },
                        label = { Text("Race Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.calendar_today), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Date: $selectedDate", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TimeField(value = hour, onValueChange = { hour = it })
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        TimeField(value = minute, onValueChange = { minute = it })
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        TimeField(value = second, onValueChange = { second = it })
                    }

                    Button(
                        onClick = {
                            if (newRaceName.isNotBlank()) {
                                try {
                                    val zdt = selectedDate.atTime(
                                        hour.toIntOrNull() ?: 0,
                                        minute.toIntOrNull() ?: 0,
                                        second.toIntOrNull() ?: 0
                                    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                                    if (editingRace != null) {
                                        viewModel.updateRace(editingRace!!.copy(name = newRaceName, startTime = zdt))
                                    } else {
                                        viewModel.addNewRace(newRaceName, zdt)
                                    }
                                    resetForm()
                                } catch (e: Exception) { /* Handle error */ }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newRaceName.isNotBlank()
                    ) {
                        Text(if (editingRace != null) "Update Race" else "Create Race")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 450.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(races, key = { it.uid }) { race ->
                val isSelected = race.uid == settings.selectedRace
                RaceCard(
                    race = race,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectRace(race.uid) },
                    onDelete = { raceToDelete = race },
                    onEdit = { startEditing(race) }
                )
            }
        }
    }
}

@Composable
fun TimeField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(55.dp).height(45.dp) // Sized for 2 digits
    ) {
        Box(contentAlignment = Alignment.Center) {
            BasicTextField(
                value = value,
                onValueChange = {
                    if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                        onValueChange(it)
                    }
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun RaceCard(
    race: com.jakubmaleszko.biegove.db.entities.Race,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit // Added this
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = race.name, style = MaterialTheme.typography.titleMedium)
                Text(text = formatUnixTimestamp(race.startTime), style = MaterialTheme.typography.bodySmall)
            }

            if (isSelected) Icon(painterResource(R.drawable.check), "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))

            // Edit Button
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.edit), "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.delete), "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}