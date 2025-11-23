package com.uirusuniki.nonumberblock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.uirusuniki.nonumberblock.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy {
        getSharedPreferences("call_blocker_prefs", MODE_PRIVATE)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupListeners()
        checkPermissions()
    }

    private fun loadSettings() {
        val isEnabled = prefs.getBoolean("blocking_enabled", false)
        val blockedCount = prefs.getInt("blocked_count", 0)

        binding.switchBlocking.isChecked = isEnabled
        binding.tvBlockedCount.text = blockedCount.toString()
        updateServiceState(isEnabled)
    }

    private fun setupListeners() {
        binding.switchBlocking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasRequiredPermissions()) {
                binding.switchBlocking.isChecked = false
                requestPermissions()
                return@setOnCheckedChangeListener
            }

            prefs.edit().putBoolean("blocking_enabled", isChecked).apply()
            updateServiceState(isChecked)

            val message = if (isChecked) "ブロック機能を有効にしました" else "ブロック機能を無効にしました"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        binding.btnClearHistory.setOnClickListener {
            prefs.edit().putInt("blocked_count", 0).apply()
            binding.tvBlockedCount.text = "0"
            Toast.makeText(this, "履歴をクリアしました", Toast.LENGTH_SHORT).show()
        }

        binding.btnRequestPermissions.setOnClickListener {
            requestPermissions()
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateServiceState(enabled: Boolean) {
        val intent = Intent(this, CallBlockerService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun checkPermissions() {
        val hasPermissions = hasRequiredPermissions()
        binding.btnRequestPermissions.isEnabled = !hasPermissions

        if (!hasPermissions) {
            binding.tvPermissionStatus.text = "権限が必要です"
            binding.tvPermissionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            binding.tvPermissionStatus.text = "権限が許可されています"
            binding.tvPermissionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissions()

            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "すべての権限が許可されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "一部の権限が拒否されました", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val blockedCount = prefs.getInt("blocked_count", 0)
        binding.tvBlockedCount.text = blockedCount.toString()
    }
}