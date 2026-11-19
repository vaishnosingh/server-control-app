package com.expapp.trigger.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.network.TriggerAction
import com.expapp.trigger.security.BiometricHelper
import com.expapp.trigger.worker.ActionTriggerWorker

/**
 * Completely translucent activity that prompts for biometric verification directly
 * on top of the home screen / launcher without ever loading or revealing the main application.
 * Dismisses instantly and executes the action via guaranteed background worker.
 */
class WidgetAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make window completely transparent & borderless
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0f)

        val deviceId = intent.getStringExtra(ServerWidgetProvider.EXTRA_DEVICE_ID)
        val actionName = intent.getStringExtra(ServerWidgetProvider.EXTRA_ACTION)

        if (deviceId.isNullOrBlank() || actionName.isNullOrBlank()) {
            finishWithoutAnimation()
            return
        }

        val action = try {
            TriggerAction.valueOf(actionName)
        } catch (_: Exception) {
            finishWithoutAnimation()
            return
        }

        val repository = DeviceRepository(SharedPrefsStorage(applicationContext))
        val device = repository.getDevices().find { it.id == deviceId }
        if (device == null) {
            finishWithoutAnimation()
            return
        }

        val actionTitle = when (action) {
            TriggerAction.POWER_ON  -> "Power On"
            TriggerAction.REBOOT    -> "Reboot"
            TriggerAction.POWER_OFF -> "Shutdown"
        }

        if (BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.authenticate(
                this,
                title = "Confirm $actionTitle",
                subtitle = "Authenticate to trigger ${device.name}"
            ) { success, errorMsg ->
                if (success) {
                    vibrateFeedback()
                    Toast.makeText(this, "⚡ Sending $actionTitle signal to ${device.name}...", Toast.LENGTH_SHORT).show()
                    ServerWidgetProvider.updateStatus(
                        applicationContext,
                        "SENDING...",
                        "⚡ Dispatched $actionTitle • Verifying..."
                    )
                    ActionTriggerWorker.enqueue(applicationContext, device.id, action, "Widget")
                } else if (errorMsg != null) {
                    Toast.makeText(this, "Trigger cancelled", Toast.LENGTH_SHORT).show()
                }
                finishWithoutAnimation()
            }
        } else {
            // Biometric hardware unavailable; dispatch directly
            vibrateFeedback()
            Toast.makeText(this, "⚡ Sending $actionTitle signal to ${device.name}...", Toast.LENGTH_SHORT).show()
            ServerWidgetProvider.updateStatus(
                applicationContext,
                "SENDING...",
                "⚡ Dispatched $actionTitle • Verifying..."
            )
            ActionTriggerWorker.enqueue(applicationContext, device.id, action, "Widget")
            finishWithoutAnimation()
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }
}
