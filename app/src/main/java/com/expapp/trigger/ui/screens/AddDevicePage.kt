package com.expapp.trigger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.data.model.FallbackConfig
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.components.AccentColorPickerRow
import com.expapp.trigger.ui.components.StatusBanner
import com.expapp.trigger.ui.theme.ServerAccents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDevicePage(
    triggerClient: TriggerClient,
    existingDeviceCount: Int = 0,
    onBack: (() -> Unit)? = null,
    onDeviceAdded: (DeviceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var r2Url          by remember { mutableStateOf("") }
    var deviceName     by remember { mutableStateOf("") }
    var nodeName       by remember { mutableStateOf("") }
    var secretKey      by remember { mutableStateOf("") }
    var accentHex      by remember(existingDeviceCount) {
        mutableStateOf(ServerAccents.nextSuggestedHex(existingDeviceCount))
    }
    var showKey        by remember { mutableStateOf(false) }
    var isFetching     by remember { mutableStateOf(false) }
    var previewLoaded  by remember { mutableStateOf(false) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var successMsg     by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Add Server",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Provision endpoints & SysPulse via R2 JSON",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusBanner(
                status = errorMsg ?: successMsg,
                isSuccess = errorMsg == null,
                onDismiss = { errorMsg = null; successMsg = null }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = r2Url,
                        onValueChange = { r2Url = it; previewLoaded = false },
                        label = { Text("Cloudflare R2 JSON URL *") },
                        placeholder = { Text(FallbackConfig.DEFAULT_R2_URL) },
                        leadingIcon = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    FilledTonalButton(
                        onClick = {
                            errorMsg = null; successMsg = null
                            if (r2Url.isBlank()) {
                                errorMsg = "Enter an R2 JSON URL first"
                                return@FilledTonalButton
                            }
                            isFetching = true
                            triggerClient.fetchR2Config(r2Url.trim()) { res ->
                                isFetching = false
                                res.onSuccess { ep ->
                                    if (deviceName.isBlank() && ep.name.isNotBlank()) deviceName = ep.name
                                    if (nodeName.isBlank() && ep.node.isNotBlank()) nodeName = ep.node
                                    if (secretKey.isBlank() && ep.secretKey.isNotBlank()) secretKey = ep.secretKey
                                    if (secretKey.isBlank() && ep.syspulseSecret.isNotBlank()) secretKey = ep.syspulseSecret
                                    if (ep.accentColor.isNotBlank()) accentHex = ep.accentColor
                                    previewLoaded = true
                                    successMsg = "R2 config verified (${if (ep.syspulse.isNotBlank()) "SysPulse detected" else "Power endpoints ready"})"
                                }.onFailure { ex ->
                                    errorMsg = "Preview failed: ${ex.message}"
                                }
                            }
                        },
                        enabled = !isFetching,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (previewLoaded) "R2 Config Loaded • Re-fetch" else "Fetch & Auto-Fill from R2")
                    }

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Server Display Name") },
                        placeholder = { Text("Auto from R2 or e.g. Home Server") },
                        leadingIcon = { Icon(Icons.Default.Storage, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = { nodeName = it },
                        label = { Text("Node Identifier") },
                        placeholder = { Text("Auto from R2 or e.g. debian") },
                        leadingIcon = { Icon(Icons.Default.Memory, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("Secret Key") },
                        placeholder = { Text("Auto from R2 or enter secret") },
                        leadingIcon = { Icon(Icons.Default.Key, null) },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Server Seed Color",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        AccentColorPickerRow(
                            selectedHex = accentHex,
                            onSelectHex = { accentHex = it }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    errorMsg = null; successMsg = null
                    if (r2Url.isBlank()) {
                        errorMsg = "Enter the Cloudflare R2 JSON URL"
                        return@Button
                    }
                    isFetching = true
                    triggerClient.fetchR2ConfigList(r2Url.trim()) { result ->
                        isFetching = false
                        result.onSuccess { list ->
                            list.forEachIndexed { i, ep ->
                                val resolvedName = when {
                                    list.size == 1 && deviceName.isNotBlank() -> deviceName.trim()
                                    ep.name.isNotBlank() -> ep.name
                                    list.size > 1 -> "Server ${existingDeviceCount + i + 1}"
                                    else -> FallbackConfig.DEFAULT_NAME
                                }
                                val resolvedNode = when {
                                    list.size == 1 && nodeName.isNotBlank() -> nodeName.trim()
                                    ep.node.isNotBlank() -> ep.node
                                    else -> FallbackConfig.DEFAULT_NODE
                                }
                                val resolvedSecret = when {
                                    secretKey.isNotBlank() -> secretKey.trim()
                                    ep.secretKey.isNotBlank() -> ep.secretKey
                                    ep.syspulseSecret.isNotBlank() -> ep.syspulseSecret
                                    else -> FallbackConfig.DEFAULT_SECRET_KEY
                                }
                                val resolvedAccent = when {
                                    ep.accentColor.isNotBlank() -> ep.accentColor
                                    list.size == 1 -> accentHex
                                    else -> ServerAccents.nextSuggestedHex(existingDeviceCount + i)
                                }
                                val dev = DeviceConfig(
                                    name              = resolvedName,
                                    node              = resolvedNode,
                                    secretKey         = resolvedSecret,
                                    r2Url             = r2Url.trim(),
                                    powerOnUrl        = ep.powerOn,
                                    powerOffUrl       = ep.powerOff,
                                    rebootUrl         = ep.reboot,
                                    statusUrl         = ep.status,
                                    syspulseUrl       = ep.syspulse,
                                    syspulseSecretKey = ep.syspulseSecret,
                                    accentColorHex    = resolvedAccent
                                )
                                onDeviceAdded(dev)
                            }
                            deviceName = ""; nodeName = ""; r2Url = ""; secretKey = ""; previewLoaded = false
                        }.onFailure { ex ->
                            errorMsg = "Failed: ${ex.message}"
                        }
                    }
                },
                enabled = !isFetching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Provisioning Server…")
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Provision Server", style = MaterialTheme.typography.titleMedium)
                }
            }

            Text(
                text = "R2 JSON supports single-server objects or a 'servers' array with poweron, poweroff, reboot, status, syspulse, syspulse_secret, name, node, and accent_color.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
