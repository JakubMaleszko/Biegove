package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

fun formatUnixTimestamp(timestamp: Long): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(timestamp))
}

@Composable
fun RaceSelectorSheet(
    viewModel: BiegoveViewModel,
    onDismiss: () -> Unit
) {
    val races by viewModel.allRaces.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current

    var newRaceName by remember { mutableStateOf("") }
    var showAddSection by remember { mutableStateOf(false) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // State for the Deletion Dialog
    var raceToDelete by remember { mutableStateOf<com.jakubmaleszko.biegove.db.entities.Race?>(null) }

    // --- DELETION CONFIRMATION DIALOG ---
    if (raceToDelete != null) {
        AlertDialog(
            onDismissRequest = { raceToDelete = null },
            title = { Text("Delete Race?") },
            text = { Text("Are you sure you want to delete '${raceToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        raceToDelete?.let { viewModel.removeRace(it) }
                        raceToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { raceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Races", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showAddSection = !showAddSection }) {
                Text(if (showAddSection) "Cancel" else "Add New")
            }
        }

        // --- ANIMATED ADD NEW RACE SECTION ---
        AnimatedVisibility(
            visible = showAddSection,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newRaceName,
                        onValueChange = { newRaceName = it },
                        label = { Text("Race Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedCard(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val newDateTime =
                                                LocalDateTime.of(year, month + 1, day, hour, minute)
                                            selectedTimestamp =
                                                newDateTime.atZone(ZoneId.systemDefault())
                                                    .toInstant().toEpochMilli()
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.calendar_today), null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                Text(formatUnixTimestamp(selectedTimestamp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newRaceName.isNotBlank()) {
                                viewModel.addNewRace(newRaceName, selectedTimestamp)
                                newRaceName = ""
                                showAddSection = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newRaceName.isNotBlank()
                    ) {
                        Icon(painterResource(R.drawable.add), null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Race")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- LIST OF RACES ---
        LazyColumn(
            modifier = Modifier.heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(races, key = { it.uid }) { race ->
                val isSelected = race.uid == settings?.selectedRace

                RaceCard(
                    race = race,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectRace(race.uid) },
                    // Instead of deleting immediately, we set the state to show the dialog
                    onDelete = { raceToDelete = race }
                )
            }
        }
    }
}

@Composable
fun RaceCard(
    race: com.jakubmaleszko.biegove.db.entities.Race,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = race.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatUnixTimestamp(race.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}