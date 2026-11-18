package com.expapp.trigger.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.expapp.trigger.BuildConfig
import com.expapp.trigger.ui.theme.AmberVibrant
import com.expapp.trigger.ui.theme.BgCard
import com.expapp.trigger.ui.theme.BgElevated
import com.expapp.trigger.ui.theme.BgSurface
import com.expapp.trigger.ui.theme.BorderSubtle
import com.expapp.trigger.ui.theme.GreenVibrant
import com.expapp.trigger.ui.theme.RedVibrant
import com.expapp.trigger.ui.theme.TextMuted
import com.expapp.trigger.ui.theme.TextPrimary
import com.expapp.trigger.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    isBiometricEnabled: Boolean,
    onBiometricToggled: (Boolean) -> Unit,
    isBgMonitorEnabled: Boolean,
    onBgMonitorToggled: (Boolean) -> Unit,
    isBiometricAvailable: Boolean,
    isNfcAvailable: Boolean,
    isNotificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenNfcWriter: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(24.dp),
            color = BgSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = GreenVibrant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SETTINGS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Biometric Setting
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isBiometricEnabled) GreenVibrant else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Biometric Security",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isBiometricAvailable) {
                                    "Require fingerprint before any power action"
                                } else {
                                    "Biometrics not supported or enrolled on this device"
                                },
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled && isBiometricAvailable,
                            onCheckedChange = { onBiometricToggled(it) },
                            enabled = isBiometricAvailable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GreenVibrant,
                                checkedTrackColor = GreenVibrant.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BgElevated
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background Monitor Setting Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isBgMonitorEnabled && isNotificationPermissionGranted) GreenVibrant else TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Server Status Alerts",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Alert outside app when servers go offline or online",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isBgMonitorEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !isNotificationPermissionGranted) {
                                        onRequestNotificationPermission()
                                    }
                                    onBgMonitorToggled(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GreenVibrant,
                                    checkedTrackColor = GreenVibrant.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BgElevated
                                )
                            )
                        }

                        // Warning banner if notification permission is blocked
                        if (isBgMonitorEnabled && !isNotificationPermissionGranted) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberVibrant.copy(alpha = 0.12f))
                                    .border(1.dp, AmberVibrant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            onRequestNotificationPermission()
                                        } else {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberVibrant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Notification permission missing. Tap to enable.",
                                        color = AmberVibrant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Send Test Notification button
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (!isNotificationPermissionGranted) {
                                    onRequestNotificationPermission()
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNotificationPermissionGranted) GreenVibrant.copy(alpha = 0.15f) else AmberVibrant.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isNotificationPermissionGranted) GreenVibrant else AmberVibrant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isNotificationPermissionGranted) "Send Test Notification" else "Grant Permission & Test",
                                color = if (isNotificationPermissionGranted) GreenVibrant else AmberVibrant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // NFC Programmer Option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable(enabled = isNfcAvailable) {
                            onDismiss()
                            onOpenNfcWriter()
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Nfc,
                            contentDescription = null,
                            tint = if (isNfcAvailable) GreenVibrant else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Program NFC Tag",
                                color = if (isNfcAvailable) TextPrimary else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isNfcAvailable) {
                                    "Tap an NFC sticker to send power signals"
                                } else {
                                    "NFC is not supported or disabled on this device"
                                },
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // App version footer
                Text(
                    text = "Server Control v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = TextMuted.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
