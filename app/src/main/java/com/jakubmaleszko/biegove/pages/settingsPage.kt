package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.jakubmaleszko.biegove.ConnectionManager
import com.jakubmaleszko.biegove.Device
import com.jakubmaleszko.biegove.MdnsHelper
import com.jakubmaleszko.biegove.R
import com.jakubmaleszko.biegove.db.AppDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val dao = db.timestampDao()
    val scope = rememberCoroutineScope()
    val devices = remember { mutableStateListOf<Device>() }
    val isConnected by ConnectionManager.isConnected.collectAsState()
    val connectedDevice by ConnectionManager.connectedDevice.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val mdns = MdnsHelper(context)
        mdns.startDiscovery { device ->
            if (devices.none { it.address == device.address }) {
                devices.add(device)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item { SectionHeader("Connection") }

            if (isConnected && connectedDevice != null) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(connectedDevice!!.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    connectedDevice!!.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { ConnectionManager.disconnect() }) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Nearby Devices") }

            if (devices.isEmpty()) {
                item {
                    Text(
                        "Searching for devices...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                    )
                }
            } else {
                items(devices) { device ->
                    val isCurrent = connectedDevice?.address == device.address
                    DeviceListItem(
                        device = device,
                        isConnected = isCurrent,
                        onConnect = {
                            scope.launch {
                                val ok = ConnectionManager.connect(device)
                                if (ok) {
                                    val entries = dao.getAll()
                                    ConnectionManager.syncData(entries.map { it.number to it.timestamp })
                                } else {
                                    snackbarHostState.showSnackbar("Connection failed — check if desktop is running")
                                }
                            }
                        }
                    )
                }
            }

            item { SectionHeader("Data & Storage") }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear all data", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "This action cannot be undone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all data") },
            text = { Text("Are you sure you want to delete all entries? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dao.deleteAll()
                            ConnectionManager.syncData(emptyList())
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

@Composable
fun DeviceListItem(device: Device, isConnected: Boolean, onConnect: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text(device.address) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isConnected) {
                Text(
                    "Connected",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                FilledTonalButton(onClick = onConnect) {
                    Text("Connect")
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp)
}