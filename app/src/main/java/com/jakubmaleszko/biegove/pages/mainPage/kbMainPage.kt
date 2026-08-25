package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.BiegoveViewModel
import kotlinx.coroutines.launch

@Composable
fun KbMainPage(
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    viewModel: BiegoveViewModel
) {
    var number by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val numberFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Centralized save function
    suspend fun handleSave() {
        val num = number.toIntOrNull()
        val noteText = note.trim().ifBlank { null }

        // Valid if at least one field is provided
        if (num != null || noteText != null) {
            viewModel.addResultToSelectedRace(num, noteText)

            val message = when {
                num != null && noteText != null -> "Added #$num with note"
                num != null -> "Added #$num"
                else -> "Added note: $noteText"
            }

            number = ""
            note = ""
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Runner Number Input
        OutlinedTextField(
            value = number,
            onValueChange = { input -> number = input.filter { it.isDigit() } },
            label = { Text("Runner ID") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(numberFocusRequester),
            keyboardActions = KeyboardActions(
                onDone = { scope.launch { handleSave() } },
                onGo = { scope.launch { handleSave() } },
                onSend = { scope.launch { handleSave() } }
            ),
            singleLine = true
        )

        // 2. Note / Description Input
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(noteFocusRequester),
            keyboardActions = KeyboardActions(
                onDone = { scope.launch { handleSave() } },
                onGo = { scope.launch { handleSave() } },
                onSend = { scope.launch { handleSave() } }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Primary Add Button
        Button(
            onClick = { scope.launch { handleSave() } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            // Enabled if either field has content
            enabled = number.isNotBlank() || note.isNotBlank()
        ) {
            Text("Add Result")
        }

        // 4. Quick "Unknown" Button
        OutlinedButton(
            onClick = {
                viewModel.addResultToSelectedRace(999999, "Unknown")
                scope.launch { snackbarHostState.showSnackbar("Added Unknown") }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Quick Unknown")
        }
    }
}