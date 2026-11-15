package com.example.phoneoptimizer

import android.app.ActivityManager
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var out: LinearLayout

    private data class Use(val total: Long, val last: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(24))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ---------- UI ----------

    private fun render() {
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "Phone Optimizer"; textSize = 24f })
        root.addView(row(deviceInfo()))

        if (!Environment.isExternalStorageManager()) {
            root.addView(btn("1. Выдать доступ ко всем файлам") {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            })
        }
        if (!hasUsageAccess()) {
            root.addView(btn("2. Выдать доступ к статистике использования") {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            })
        }

        root.addView(btn("Найти большие файлы (от 50 МБ)") { scanFiles() })
        root.addView(btn("Показать приложения (редко используемые сверху)") { showApps() })
        root.addView(btn("Shizuku: подключить") { connectShizuku() })
        root.addView(btn("Shizuku: остановить приложения, не открывавшиеся сутки") { stopUnused() })

        out = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(out)
    }

    private fun deviceInfo(): String {
        val st = StatFs(Environment.getDataDirectory().path)
        val freeStorage = Formatter.formatFileSize(this, st.availableBytes)
        val totalStorage = Formatter.formatFileSize(this, st.totalBytes)

        val mi = ActivityManager.MemoryInfo()
        getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        val freeRam = Formatter.formatFileSize(this, mi.availMem)
        val totalRam = Formatter.formatFileSize(this, mi.totalMem)

        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0

        return "Память: свободно $freeStorage из $totalStorage\n" +
            "ОЗУ: свободно $freeRam из $totalRam\n" +
            "Батарея: $level%, $temp °C"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun row(text: String, onClick: (() -> Unit)? = null) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setPadding(0, dp(10), 0, dp(10))
        if (onClick != null) setOnClickListener { onClick()
