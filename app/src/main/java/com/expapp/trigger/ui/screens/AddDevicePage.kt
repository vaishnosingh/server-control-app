package com.expapp.trigger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expapp.trigger.data.model.DeviceConfig
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.components.StatusBanner
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderDefault
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun AddDevicePage(
    triggerClient: TriggerClient,
    onDeviceAdded: (DeviceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var deviceName by remember { mutableStateOf("") }
    var nodeName   by remember { mutableStateOf("debian") }
    var r2Url      by remember { mutableStateOf("") }
    var secretKey  by remember { mutableStateOf("") }
    var showKey    by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(GreenVibrant.copy(alpha = 0.12f))
                    .border(1.dp, GreenVibrant.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = GreenVibrant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    "Add Device",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Configure via Cloudflare R2 JSON",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        StatusBanner(
            status = errorMsg ?: successMsg,
            isSuccess = errorMsg == null,
            onDismiss = { errorMsg = null; successMsg = null }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Form card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(BgElevated, BgSurface)))
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor       = GreenVibrant,
                unfocusedBorderColor     = BorderDefault,
                focusedLabelColor        = GreenVibrant,
                unfocusedLabelColor      = TextSecondary,
                cursorColor              = GreenVibrant,
                focusedTextColor         = TextPrimary,
                unfocusedTextColor       = TextPrimary,
                focusedPlaceholderColor  = TextMuted,
                unfocusedPlaceholderColor= TextMuted
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = deviceName, onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    placeholder = { Text("e.g. Home Server") },
                    leadingIcon = { Icon(Icons.Default.Storage, null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = nodeName, onValueChange = { nodeName = it },
                    label = { Text("Node Identifier") },
                    placeholder = { Text("e.g. debian") },
                    leadingIcon = { Icon(Icons.Default.Memory, null, tint = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = r2Url, onValueChange = { r2Url = it },
                    label = { Text("Cloudflare R2 JSON URL") },
                    placeholder = { Text("https://pub-xxx.r2.dev/config.json") },
                    leadingIcon = { Icon(Icons.Default.Link, null, tint = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = secretKey, onValueChange = { secretKey = it },
                    label = { Text("Secret Key") },
                    placeholder = { Text("Your API secret") },
                    leadingIcon = { Icon(Icons.Default.Key, null, tint = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMsg = null; successMsg = null
                when {
                    deviceName.isBlank() -> { errorMsg = "Enter a device name"; return@Button }
                    r2Url.isBlank()      -> { errorMsg = "Enter the Cloudflare R2 URL"; return@Button }
                    secretKey.isBlank()  -> { errorMsg = "Enter the secret key"; return@Button }
                }
                isFetching = true
                triggerClient.fetchR2Config(r2Url.trim()) { result ->
                    isFetching = false
                    result.onSuccess { ep ->
                        val dev = DeviceConfig(
                            name       = deviceName.trim(),
                            node       = nodeName.trim().ifBlank { "debian" },
                            secretKey  = secretKey.trim(),
                            r2Url      = r2Url.trim(),
                            powerOnUrl = ep.powerOn,
                            powerOffUrl= ep.powerOff,
                            rebootUrl  = ep.reboot,
                            statusUrl  = ep.status
                        )
                        onDeviceAdded(dev)
                        successMsg = "\"${dev.name}\" added successfully"
                        deviceName = ""; r2Url = ""; secretKey = ""
                    }.onFailure { ex ->
                        errorMsg = "Failed: ${ex.message}"
                    }
                }
            },
            enabled = !isFetching,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor        = GreenVibrant.copy(alpha = 0.15f),
                contentColor          = GreenVibrant,
                disabledContainerColor= GreenVibrant.copy(alpha = 0.05f),
                disabledContentColor  = GreenVibrant.copy(alpha = 0.4f)
            )
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    color     = GreenVibrant,
                    modifier  = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    trackColor  = GreenVibrant.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Fetching config…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Device", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "The JSON config must include: poweron, poweroff, reboot, and status URL keys.",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 16.sp
        )
    }
}
