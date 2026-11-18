package com.expapp.trigger.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.ui.theme.PowerOffRed
import com.expapp.trigger.ui.theme.RebootAmber

@Composable
fun ActionConfirmDialog(
    action: TriggerAction,
    deviceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionName = when (action) {
        TriggerAction.POWER_ON -> "Power On"
        TriggerAction.POWER_OFF -> "Power Off"
        TriggerAction.REBOOT -> "Reboot"
    }

    val confirmButtonColor = when (action) {
        TriggerAction.POWER_OFF -> PowerOffRed
        TriggerAction.REBOOT -> RebootAmber
        TriggerAction.POWER_ON -> Color(0xFF2E7D32)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Confirm $actionName") },
        text = {
            Text(text = "Are you sure you want to send $actionName to \"$deviceName\"?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor)
            ) {
                Text(text = actionName, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
