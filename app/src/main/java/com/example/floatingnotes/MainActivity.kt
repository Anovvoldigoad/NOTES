package com.example.floatingnotes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var permissionText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionText.isInitialized) refreshState()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }
        scroll.addView(content)

        val title = TextView(this).apply {
            text = "Floating Notes"
            textSize = 28f
            setTextColor(Color.rgb(35, 35, 35))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        content.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "Catatan kecil yang selalu terlihat di atas aplikasi lain"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(24))
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        permissionText = TextView(this).apply {
            textSize = 15f
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.rgb(245, 245, 245))
        }
        content.addView(permissionText, LinearLayout.LayoutParams(-1, -2))

        val permissionButton = Button(this).apply {
            text = "Buka izin tampil di atas aplikasi"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        }
        content.addView(permissionButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        startButton = Button(this).apply {
            text = "Tampilkan catatan"
            setOnClickListener { startOverlay() }
        }
        content.addView(startButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        stopButton = Button(this).apply {
            text = "Sembunyikan catatan"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                Toast.makeText(this@MainActivity, "Catatan disembunyikan", Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(stopButton, LinearLayout.LayoutParams(-1, -2))

        val help = TextView(this).apply {
            text = "Cara pakai:\n• Seret bilah judul untuk memindahkan catatan.\n• Seret sudut kanan bawah untuk mengubah ukuran.\n• Ketuk area putih untuk mengetik; keyboard akan dibuka otomatis.\n• Isi, posisi, dan ukuran tersimpan saat aplikasi ditutup."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(24), 0, 0)
        }
        content.addView(help, LinearLayout.LayoutParams(-1, -2))
        setContentView(scroll)
        refreshState()
    }

    private fun refreshState() {
        val allowed = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)
        permissionText.text = if (allowed) {
            "Izin overlay: AKTIF\nAnda dapat menampilkan catatan di atas aplikasi lain."
        } else {
            "Izin overlay: BELUM AKTIF\nAktifkan izin ini agar catatan dapat muncul di atas aplikasi lain."
        }
        startButton.isEnabled = allowed
        stopButton.isEnabled = true
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Aktifkan izin overlay terlebih dahulu", Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        Toast.makeText(this, "Catatan tampil di layar", Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
