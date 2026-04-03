package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalFocusManager
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
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Centralized save function
    suspend fun saveTimestamp(targetId: Int?) {
        if (targetId == null) return

        viewModel.addResultToSelectedRace(targetId)
        number = ""
        snackbarHostState.showSnackbar("Added ID: $targetId")
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp), // Moved lower down
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                .focusRequester(focusRequester),
            keyboardActions = KeyboardActions(onDone = {
                scope.launch { saveTimestamp(number.toIntOrNull()) }
            })
        )

        Button(
            onClick = { scope.launch { saveTimestamp(number.toIntOrNull()) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Add Runner")
        }

        OutlinedButton(
            onClick = { scope.launch { saveTimestamp(999999) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Add Unknown")
        }
    }
}