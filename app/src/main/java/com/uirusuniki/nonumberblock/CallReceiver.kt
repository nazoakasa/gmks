package com.uirusuniki.nonumberblock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
        private const val NOTIFICATION_CHANNEL_ID = "blocked_calls_channel"
        private var lastBlockedTime: Long = 0
        private var notificationId = 1000
        private var nullCount = 0 // nullが来た回数をカウント
        private var hasReceivedNumber = false // 番号を受信したか
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val prefs = context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("blocking_enabled", false)

        if (!isEnabled) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                Log.d(TAG, "着信検出: $incomingNumber (nullCount=$nullCount, hasReceivedNumber=$hasReceivedNumber)")

                if (!incomingNumber.isNullOrEmpty()) {
                    // 番号が取得できた = 通常の着信
                    Log.d(TAG, "通常の着信: $incomingNumber")
                    hasReceivedNumber = true
                } else {
                    // nullをカウント
                    nullCount++

                    // 2回目以降のnullで、まだ番号が来ていない場合は非通知
                    if (nullCount >= 2 && !hasReceivedNumber) {
                        Log.d(TAG, "非通知と判定 (nullが${nullCount}回)")
                        checkAndBlock(context)
                    } else {
                        Log.d(TAG, "nullを検出 (${nullCount}回目) - 待機中")
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // 通話終了時にリセット
                nullCount = 0
                hasReceivedNumber = false
                Log.d(TAG, "通話終了 - リセット")
            }
        }
    }

    private fun checkAndBlock(context: Context) {
        // 重複ブロック防止
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastBlockedTime < 3000) {
            Log.d(TAG, "重複ブロック検出 - スキップします")
            return
        }

        lastBlockedTime = currentTime

        Log.d(TAG, "非通知着信を確認 - ブロックします")
        blockCall(context)

        val prefs = context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("blocked_count", 0)
        prefs.edit().putInt("blocked_count", currentCount + 1).apply()

        showBlockedNotification(context, currentCount + 1)
    }

    private fun blockCall(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val endCallResult = telecomManager.endCall()
                Log.d(TAG, "通話終了結果: $endCallResult")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通話のブロックに失敗", e)
        }
    }

    private fun showBlockedNotification(context: Context, totalBlocked: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "ブロックした電話",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "非通知電話をブロックした時の通知"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.JAPAN)
        val currentTime = timeFormat.format(Date())

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("非通知電話をブロックしました")
            .setContentText("時刻: $currentTime | 累計: ${totalBlocked}件")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(notificationId++, notification)
    }
}