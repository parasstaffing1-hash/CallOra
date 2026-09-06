package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableSharedFlow

class WhatsAppCallDetectorService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (!pkg.contains("whatsapp", ignoreCase = true)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val category = notification.category

        val isCall = category == Notification.CATEGORY_CALL ||
                title.contains("call", ignoreCase = true) ||
                text.contains("call", ignoreCase = true) ||
                text.contains("incoming", ignoreCase = true) ||
                text.contains("ongoing", ignoreCase = true) ||
                text.contains("calling", ignoreCase = true) ||
                subText.contains("call", ignoreCase = true)

        if (isCall) {
            val contactName = if (title.isNotBlank()) title else "WhatsApp Client"
            detectedEvents.tryEmit("WhatsApp call with $contactName")

            val prefs = getSharedPreferences("agency_settings", Context.MODE_PRIVATE)
            val autoRecord = prefs.getBoolean("auto_record_whatsapp", false)

            if (autoRecord) {
                // Auto-trigger recording in CallRecordingService
                val serviceIntent = Intent(this, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_START_WHATSAPP_RECORDING
                    putExtra("client_name", contactName)
                    putExtra("client_company", "WhatsApp Call")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                // Show prompt heads-up notification with one-tap start recording
                showCallDetectedPrompt(contactName)
            }
        }
    }

    private fun showCallDetectedPrompt(contactName: String) {
        val channelId = "whatsapp_call_detector_channel"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WhatsApp Call Detection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a WhatsApp call is active to start recording"
            }
            manager.createNotificationChannel(channel)
        }

        // Action intent to start recording directly
        val startRecordIntent = Intent(this, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_START_WHATSAPP_RECORDING
            putExtra("client_name", contactName)
            putExtra("client_company", "WhatsApp Call")
        }
        val startRecordPending = PendingIntent.getService(
            this, 101, startRecordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification to open MainActivity
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPending = PendingIntent.getActivity(
            this, 102, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val promptNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("📞 Active WhatsApp Call Detected")
            .setContentText("Contact: $contactName • Tap below to record with VoIP engine")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(appPending)
            .addAction(android.R.drawable.ic_btn_speak_now, "⏺️ Start Recording", startRecordPending)
            .build()

        manager.notify(PROMPT_NOTIF_ID, promptNotification)
    }

    companion object {
        const val PROMPT_NOTIF_ID = 2002
        val detectedEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
