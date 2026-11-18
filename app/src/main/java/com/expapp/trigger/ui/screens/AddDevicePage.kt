package com.expapp.trigger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.expapp.trigger.ui.theme.DarkCardBorder
import com.expapp.trigger.ui.theme.DarkSurface
import com.expapp.trigger.ui.theme.PowerOnGreen
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun AddDevicePage(
    triggerClient: TriggerClient,
    onDeviceAdded: (DeviceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var deviceName by remember { mutableStateOf("") }
    var nodeName by remember { mutableStateOf("debian") }
    var r2Url by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = PowerOnGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add Remote Device",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Provide your Cloudflare R2 JSON endpoint URL and authentication key to register a new remote trigger.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                StatusBanner(
                    status = errorMessage ?: successMessage,
                    isSuccess = errorMessage == null,
                    onDismiss = {
                        errorMessage = null
                        successMessage = null
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Device Name Field
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Friendly Name") },
                    placeholder = { Text("e.g. Living Room Server") },
                    leadingIcon = {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerOnGreen,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Node Identifier Field
                OutlinedTextField(
                    value = nodeName,
                    onValueChange = { nodeName = it },
                    label = { Text("Node Identifier") },
                    placeholder = { Text("e.g. debian or pop-os") },
                    leadingIcon = {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerOnGreen,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cloudflare R2 JSON URL Field
                OutlinedTextField(
                    value = r2Url,
                    onValueChange = { r2Url = it },
                    label = { Text("Cloudflare R2 JSON URL") },
                    placeholder = { Text("https://.../endpoints.json") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerOnGreen,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Secret Key Field
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("Secret Key") },
                    placeholder = { Text("Carbon@12") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerOnGreen,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fetch & Add Button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null

                        if (deviceName.isBlank()) {
                            errorMessage = "Please enter a device name"
                            return@Button
                        }
                        if (r2Url.isBlank()) {
                            errorMessage = "Please provide the Cloudflare R2 JSON URL"
                            return@Button
                        }
                        if (secretKey.isBlank()) {
                            errorMessage = "Please enter the secret key for this device"
                            return@Button
                        }

                        isFetching = true
                        triggerClient.fetchR2Config(r2Url.trim()) { result ->
                            isFetching = false
                            result.onSuccess { endpoints ->
                                val newDevice = DeviceConfig(
                                    name = deviceName.trim(),
                                    node = if (nodeName.isBlank()) "debian" else nodeName.trim(),
                                    secretKey = secretKey.trim(),
                                    r2Url = r2Url.trim(),
                                    powerOnUrl = endpoints.powerOn,
                                    powerOffUrl = endpoints.powerOff,
                                    rebootUrl = endpoints.reboot
                                )
                                onDeviceAdded(newDevice)
                                successMessage = "Device \"${newDevice.name}\" added successfully!"
                                deviceName = ""
                                r2Url = ""
                                secretKey = ""
                            }.onFailure { ex ->
                                errorMessage = "Failed to fetch endpoints: ${ex.message}"
                            }
                        }
                    },
                    enabled = !isFetching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PowerOnGreen)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Fetching Endpoints...", color = Color.White)
                    } else {
                        Text(
                            text = "Fetch & Add Device",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
