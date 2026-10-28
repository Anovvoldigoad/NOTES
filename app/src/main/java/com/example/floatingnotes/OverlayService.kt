package com.example.floatingnotes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.InputDevice
import android.view.InputMethodManager
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher

class OverlayService : Service() {
    companion object {
        private const val CHANNEL_ID = "floating_notes"
        private const val NOTIFICATION_ID = 42
        private const val PREFS = "note_state"
        private const val KEY_TEXT = "text"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val ACTION_STOP = "com.example.floatingnotes.STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private var overlay: FrameLayout? = null
    private var editText: EditText? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var resizeHandle: View? = null
    private var density = 1f

    override fun onCreate() {
        super.onCreate()
        density = resources.displayMetrics.density
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startAsForeground()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        saveState()
        overlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addOverlay() {
        if (overlay != null) return
        val root = FrameLayout(this).apply {
            isFocusableInTouchMode = true
            setPadding(0, 0, 0, 0)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(com.example.floatingnotes.R.drawable.overlay_header)
            setPadding(dp(12), 0, dp(4), 0)
        }
        val title = TextView(this).apply {
            text = "Floating Note"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        val close = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Tutup catatan"
            setOnClickListener { stopSelf() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(42), dp(42)))
        root.addView(header, FrameLayout.LayoutParams(-1, dp(42), Gravity.TOP))

        val editor = EditText(this).apply {
            setText(prefs.getString(KEY_TEXT, "") ?: "")
            setTextColor(Color.rgb(25, 25, 25))
            setHintTextColor(Color.rgb(120, 120, 120))
            hint = "Ketuk di sini untuk mengetik..."
            textSize = 15f
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundResource(com.example.floatingnotes.R.drawable.overlay_body)
            setSingleLine(false)
            isVerticalScrollBarEnabled = true
            isScrollbarFadingEnabled = true
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    prefs.edit().putString(KEY_TEXT, s?.toString().orEmpty()).apply()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) view.post { showKeyboard(view) }
            }
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    view.requestFocus()
                    view.post { showKeyboard(view) }
                }
                false
            }
        }
        editText = editor
        val editorParams = FrameLayout.LayoutParams(-1, -1).apply {
            topMargin = dp(42)
            rightMargin = dp(8)
            bottomMargin = dp(8)
        }
        root.addView(editor, editorParams)

        val handle = View(this).apply {
            setBackgroundResource(com.example.floatingnotes.R.drawable.resize_handle)
            contentDescription = "Ubah ukuran catatan"
        }
        resizeHandle = handle
        val handleParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END)
        root.addView(handle, handleParams)

        val savedWidth = prefs.getInt(KEY_WIDTH, dp(320))
        val savedHeight = prefs.getInt(KEY_HEIGHT, dp(230))
        val displayWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        val params = WindowManager.LayoutParams(
            savedWidth.coerceIn(dp(240), (displayWidth - dp(16)).coerceAtLeast(dp(240))),
            savedHeight.coerceIn(dp(170), (displayHeight - dp(32)).coerceAtLeast(dp(170))),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_X, dp(16)).coerceAtLeast(0)
            y = prefs.getInt(KEY_Y, dp(100)).coerceAtLeast(0)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            title = "Floating Notes"
        }
        layoutParams = params
        attachDrag(header)
        attachResize(handle)
        windowManager.addView(root, params)
        overlay = root
    }

    private fun attachDrag(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (dx != 0 || dy != 0) moved = true
                        params.x = (startX + dx).coerceAtLeast(0)
                        params.y = (startY + dy).coerceAtLeast(0)
                        updateWindow()
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        saveState()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun attachResize(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startWidth = 0
            private var startHeight = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = params.width
                        startHeight = params.height
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val maxWidth = resources.displayMetrics.widthPixels - params.x - dp(8)
                        val maxHeight = resources.displayMetrics.heightPixels - params.y - dp(8)
                        params.width = (startWidth + (event.rawX - downX).toInt()).coerceIn(dp(240), maxWidth.coerceAtLeast(dp(240)))
                        params.height = (startHeight + (event.rawY - downY).toInt()).coerceIn(dp(170), maxHeight.coerceAtLeast(dp(170)))
                        updateWindow()
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        saveState()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun updateWindow() {
        val view = overlay ?: return
        val params = layoutParams ?: return
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun saveState() {
        val params = layoutParams ?: return
        prefs.edit()
            .putInt(KEY_X, params.x)
            .putInt(KEY_Y, params.y)
            .putInt(KEY_WIDTH, params.width)
            .putInt(KEY_HEIGHT, params.height)
            .putString(KEY_TEXT, editText?.text?.toString().orEmpty())
            .apply()
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Floating Notes", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).setAction(ACTION_STOP)
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val stopPending = PendingIntent.getService(this, 1, stopIntent, pendingFlags)
        val launchPending = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java), pendingFlags
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.example.floatingnotes.R.drawable.ic_note)
                .setContentTitle("Floating Notes aktif")
                .setContentText("Ketuk untuk membuka pengaturan catatan")
                .setContentIntent(launchPending)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, "Sembunyikan", stopPending).build())
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(com.example.floatingnotes.R.drawable.ic_note)
                .setContentTitle("Floating Notes aktif")
                .setContentText("Catatan tampil di atas aplikasi lain")
                .setContentIntent(launchPending)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, "Sembunyikan", stopPending).build())
                .build()
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
