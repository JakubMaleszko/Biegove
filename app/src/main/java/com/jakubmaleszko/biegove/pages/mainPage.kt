package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jakubmaleszko.biegove.R
import com.jakubmaleszko.biegove.db.AppDatabase
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(onNavigateToTable: () -> Unit, onNavigateToSettings: () -> Unit) {
    val db = AppDatabase.getInstance(LocalContext.current)
    val dao = db.timestampDao()
    var number by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun saveTimestamp() {
        val num = number.toIntOrNull() ?: return
        val timestamp = System.currentTimeMillis()
        val entity = Timestamp(
            uid = 0,
            number = num,
            timestamp = timestamp
        )
        dao.insert(entity)
        number = ""
        snackbarHostState.showSnackbar("Added entry")
    }

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
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth()  ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {
            OutlinedTextField(
                value = number,
                onValueChange = { input ->
                    number = input.filter { it.isDigit() }
                },
                label = { Text("ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,imeAction = ImeAction.Done),
                modifier = Modifier.focusRequester(focusRequester),
                keyboardActions = KeyboardActions(onDone = {
                    scope.launch {
                        saveTimestamp()
                    }
                })
            )
            Button(onClick = {
                scope.launch {
                    saveTimestamp()
                }
            }) {
                Text("Add")
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
