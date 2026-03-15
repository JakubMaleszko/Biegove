package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.jakubmaleszko.biegove.BiegoveViewModel
import com.jakubmaleszko.biegove.ConnectionManager
import com.jakubmaleszko.biegove.Device
import com.jakubmaleszko.biegove.MdnsHelper
import com.jakubmaleszko.biegove.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(onBack: () -> Unit, viewModel: BiegoveViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State
    val devices = remember { mutableStateListOf<Device>() }
    val connectedDevice by ConnectionManager.connectedDevice.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    // MDNS Logic
    val mdns = remember { MdnsHelper(context) }
    LaunchedEffect(Unit) {
        while (isActive) {
            mdns.startDiscovery { device ->
                if (devices.none { it.address == device.address }) devices.add(device)
            }
            delay(3000)
        }
    }
    DisposableEffect(Unit) { onDispose { scope.launch(Dispatchers.IO) { mdns.stopDiscovery() } } }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Settings") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(painterResource(R.drawable.arrow_back), "Back") }
            })
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {

            item { SectionHeader("Connection") }
            if (devices.isEmpty() && connectedDevice == null) {
                item { Text("Searching for devices...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            } else {
                items(devices) { device ->
                    val isCurrent = connectedDevice?.address == device.address
                    DeviceListItem(
                        device = device,
                        isConnected = isCurrent,
                        onConnect = {
                            scope.launch {
                                if (ConnectionManager.connect(device)) {
                                    val activeRace = viewModel.selectedRaceObject.value
                                    val entries = viewModel.currentRaceResults.value

                                    if (activeRace != null) {
                                        ConnectionManager.syncData(
                                            startTime = activeRace.startTime,
                                            entries = entries.map { it.number to it.time }
                                        )
                                        snackbarHostState.showSnackbar("Connected and Synced: ${activeRace.name}")
                                    } else {
                                        ConnectionManager.syncData(0L, emptyList())
                                        snackbarHostState.showSnackbar("Connected (No active race to sync)")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Connection failed")
                                }
                            }
                        },
                        onDisconnect = { ConnectionManager.disconnect() }
                    )
                }
            }

            item { SectionHeader("Appearance") }

            // Theme Item
            item {
                SettingsClickableItem(
                    title = "App Theme",
                    subtitle = listOf("System", "Light", "Dark")[settings?.themeMode ?: 0],
                    onClick = { showThemeSheet = true }
                )
            }

            // Draw Switch
            item {
                SettingsSwitchItem(
                    title = "Use hand draw",
                    checked = settings?.useDraw ?: false,
                    onCheckedChange = { viewModel.toggleDraw(it) }
                )
            }

            item { SectionHeader("Data & Storage") }
            item {
                SettingsClickableItem(
                    title = "Clear all data",
                    subtitle = "This action cannot be undone",
                    isError = true,
                    onClick = { showClearDialog = true }
                )
            }
        }
    }

    // --- Bottom Sheets & Dialogs ---

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
                Text("Choose Theme", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                listOf("System", "Light", "Dark").forEachIndexed { index, label ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).selectable(
                            selected = (settings?.themeMode ?: 0) == index,
                            onClick = { viewModel.updateTheme(index); showThemeSheet = false }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (settings?.themeMode ?: 0) == index, onClick = null)
                        Text(label, Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all data") },
            text = { Text("This will delete all races and all runner results. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.clearAllRaces()
                        ConnectionManager.syncData(0L,emptyList())
                        showClearDialog = false
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear Everything") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

// --- Minified Reusable Components ---

@Composable
fun DeviceListItem(device: Device, isConnected: Boolean, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text(device.address) },
        trailingContent = {
            if (isConnected) {
                OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
            } else {
                FilledTonalButton(onClick = onConnect) { Text("Connect") }
            }
        },
        leadingContent = {
            if (isConnected) Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            else Icon(painterResource(R.drawable.settings), null)
        }
    )
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String, isError: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
    )
}