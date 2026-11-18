package com.expapp.trigger.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import com.expapp.trigger.network.TriggerAction

data class NfcTriggerPayload(
    val nodeOrDeviceId: String,
    val action: TriggerAction
)

object NfcHelper {

    fun isNfcSupported(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return adapter != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return adapter != null && adapter.isEnabled
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
        )

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            addDataScheme("expapp")
        }
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        adapter.enableForegroundDispatch(activity, pendingIntent, arrayOf(ndefFilter, tagFilter), null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableForegroundDispatch(activity)
    }

    fun createNdefMessage(nodeOrDeviceId: String, action: TriggerAction): NdefMessage {
        val uriString = "expapp://trigger?device=$nodeOrDeviceId&action=${action.name.lowercase()}"
        val uriRecord = NdefRecord.createUri(Uri.parse(uriString))
        val aarRecord = NdefRecord.createApplicationRecord("com.expapp.trigger")
        return NdefMessage(arrayOf(uriRecord, aarRecord))
    }

    fun writeTag(tag: Tag, message: NdefMessage): Result<Unit> {
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(Exception("NFC Tag is read-only"))
                }
                if (ndef.maxSize < message.byteArrayLength) {
                    ndef.close()
                    return Result.failure(Exception("Tag capacity too small (${ndef.maxSize} bytes < ${message.byteArrayLength} bytes)"))
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                Result.success(Unit)
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Tag does not support NDEF format"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseTriggerPayload(intent: Intent): NfcTriggerPayload? {
        val uri: Uri? = intent.data ?: run {
            // Check NdefMessage extra
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val ndefMessage = rawMessages[0] as? NdefMessage
                val record = ndefMessage?.records?.firstOrNull()
                record?.toUri()
            } else null
        }

        if (uri != null && uri.scheme == "expapp" && uri.host == "trigger") {
            val device = uri.getQueryParameter("device") ?: return null
            val actionStr = uri.getQueryParameter("action") ?: return null
            val action = when (actionStr.lowercase()) {
                "power_on", "on", "poweron" -> TriggerAction.POWER_ON
                "power_off", "off", "poweroff", "shutdown" -> TriggerAction.POWER_OFF
                "reboot", "restart" -> TriggerAction.REBOOT
                else -> null
            } ?: return null
            return NfcTriggerPayload(device, action)
        }
        return null
    }
}
