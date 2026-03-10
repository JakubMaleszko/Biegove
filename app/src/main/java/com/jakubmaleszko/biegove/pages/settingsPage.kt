package com.jakubmaleszko.biegove.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.jakubmaleszko.biegove.Device
import com.jakubmaleszko.biegove.MdnsHelper
import com.jakubmaleszko.biegove.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val devices = remember { mutableStateListOf<Device>() }

    LaunchedEffect(Unit) {
        val mdns = MdnsHelper(context)
        mdns.startDiscovery { device ->
            if (devices.none { it.address == device.address }) {
                devices.add(device)
            }
        }
    }

    Scaffold(
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
            // Section: Devices
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
                    DeviceListItem(device = device)
                }
            }

            // Section: Data Management
            item { SectionHeader("Data & Storage") }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
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
                            onClick = { /* Handle Clear */ },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge, // Smaller, uppercase-style feel
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp)
    )
}

@Composable
fun DeviceListItem(device: Device) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text(device.address) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.calendar_today),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            FilledTonalButton(onClick = { /* Connect */ }) {
                Text("Connect")
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp)
}