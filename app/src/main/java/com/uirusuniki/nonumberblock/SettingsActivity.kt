package com.uirusuniki.nonumberblock

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uirusuniki.nonumberblock.databinding.ActivitySettingsBinding
import com.google.android.material.chip.Chip

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs by lazy {
        getSharedPreferences("call_blocker_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadBlockedItems()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "ブロック設定"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadBlockedItems() {
        binding.chipGroupBlocked.removeAllViews()

        val blockedNumbers = getBlockedNumbers()
        val blockedPrefixes = getBlockedPrefixes()
        val blockedCountryCodes = getBlockedCountryCodes()

        // 番号
        blockedNumbers.forEach { number ->
            addChip("番号: $number", number, BlockType.NUMBER)
        }

        // 市外局番
        blockedPrefixes.forEach { prefix ->
            addChip("市外局番: $prefix", prefix, BlockType.PREFIX)
        }

        // 国コード
        blockedCountryCodes.forEach { code ->
            addChip("国: $code", code, BlockType.COUNTRY)
        }
    }

    private fun addChip(label: String, value: String, type: BlockType) {
        val chip = Chip(this).apply {
            text = label
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                removeBlockedItem(value, type)
                binding.chipGroupBlocked.removeView(this)
            }
        }
        binding.chipGroupBlocked.addView(chip)
    }

    private fun setupListeners() {
        binding.btnAddNumber.setOnClickListener {
            showAddDialog(BlockType.NUMBER, "電話番号を追加", "例: 09012345678")
        }

        binding.btnAddPrefix.setOnClickListener {
            showAddDialog(BlockType.PREFIX, "市外局番を追加", "例: 03, 06, 0120")
        }

        binding.btnAddCountry.setOnClickListener {
            showAddDialog(BlockType.COUNTRY, "国コードを追加", "例: +81, +1, +86")
        }
    }

    private fun showAddDialog(type: BlockType, title: String, hint: String) {
        val input = android.widget.EditText(this).apply {
            setHint(hint)
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("追加") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    addBlockedItem(value, type)
                    loadBlockedItems()
                    Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun addBlockedItem(value: String, type: BlockType) {
        val key = when (type) {
            BlockType.NUMBER -> "blocked_numbers"
            BlockType.PREFIX -> "blocked_prefixes"
            BlockType.COUNTRY -> "blocked_country_codes"
        }

        val current = prefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
        val updated = current.toMutableSet().apply { add(value) }
        prefs.edit().putStringSet(key, updated).apply()
    }

    private fun removeBlockedItem(value: String, type: BlockType) {
        val key = when (type) {
            BlockType.NUMBER -> "blocked_numbers"
            BlockType.PREFIX -> "blocked_prefixes"
            BlockType.COUNTRY -> "blocked_country_codes"
        }

        val current = prefs.getStringSet(key, mutableSetOf()) ?: mutableSetOf()
        val updated = current.toMutableSet().apply { remove(value) }
        prefs.edit().putStringSet(key, updated).apply()
    }

    private fun getBlockedNumbers(): Set<String> {
        return prefs.getStringSet("blocked_numbers", setOf()) ?: setOf()
    }

    private fun getBlockedPrefixes(): Set<String> {
        return prefs.getStringSet("blocked_prefixes", setOf()) ?: setOf()
    }

    private fun getBlockedCountryCodes(): Set<String> {
        return prefs.getStringSet("blocked_country_codes", setOf()) ?: setOf()
    }

    enum class BlockType {
        NUMBER, PREFIX, COUNTRY
    }
}