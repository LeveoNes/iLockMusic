package com.example.ilockmusic

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var manager: MediaSessionManager
    private var controller: MediaController? = null
    private var seeking = false
    private lateinit var art: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var play: Button
    private lateinit var progress: SeekBar
    private lateinit var elapsed: TextView
    private lateinit var duration: TextView
    private lateinit var status: TextView
    private val yandexPackages = setOf("ru.yandex.music", "ru.yandex.music.beta")

    private fun label(value: String, size: Float, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.WHITE)
        gravity = Gravity.CENTER; if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
    private fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        window.decorView.setBackgroundColor(Color.rgb(17, 20, 30))
        val root = column().apply { setPadding(dp(24), dp(20), dp(24), dp(24)) }
        val scroll = ScrollView(this).apply { fillViewport = true; addView(root) }
        setContentView(scroll)
        root.addView(label("iLock Music", 17, true))
        root.addView(label("Яндекс Музыка", 13).apply { alpha = .65f })
        val spacer = View(this); root.addView(spacer, LinearLayout.LayoutParams(1, dp(35)))
        art = ImageView(this).apply { setBackgroundColor(Color.rgb(44, 49, 68)); scaleType = ImageView.ScaleType.CENTER_CROP }
        val side = (resources.displayMetrics.widthPixels - dp(48)).coerceAtMost(dp(340))
        root.addView(art, LinearLayout.LayoutParams(side, side))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(25)))
        title = label("Музыка не запущена", 22, true); root.addView(title)
        artist = label("Открой Яндекс Музыку", 15).apply { alpha = .7f }; root.addView(artist)
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(25)))
        progress = SeekBar(this); root.addView(progress, LinearLayout.LayoutParams(-1, dp(42)))
        val times = row(); elapsed = label("0:00", 12); duration = label("0:00", 12)
        times.addView(elapsed); times.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)); times.addView(duration)
        root.addView(times, LinearLayout.LayoutParams(-1, -2))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        val controls = row()
        val previous = Button(this).apply { text = "⏮"; setOnClickListener { controller?.transportControls?.skipToPrevious() } }
        play = Button(this).apply { text = "▶"; setOnClickListener {
            if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) controller?.transportControls?.pause()
            else controller?.transportControls?.play()
        } }
        val next = Button(this).apply { text = "⏭"; setOnClickListener { controller?.transportControls?.skipToNext() } }
        listOf(previous, play, next).forEach { controls.addView(it, LinearLayout.LayoutParams(0, dp(60), 1f)) }
        root.addView(controls, LinearLayout.LayoutParams(-1, -2))
        status = label("", 13).apply { alpha = .75f }; root.addView(status)
        val access = Button(this).apply { text = "Разрешить доступ к уведомлениям"; setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } }
        root.addView(access)
        progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar: SeekBar) { seeking = true }
            override fun onStopTrackingTouch(bar: SeekBar) { controller?.transportControls?.seekTo(bar.progress.toLong()); seeking = false }
            override fun onProgressChanged(bar: SeekBar, p: Int, user: Boolean) { if (user) elapsed.text = time(p.toLong()) }
        })
        handler.post(object : Runnable { override fun run() { refresh(); handler.postDelayed(this, 1000) } })
    }
    private fun time(ms: Long): String { val sec = (ms / 1000).coerceAtLeast(0); return "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}" }
    private fun refresh() {
        val listener = ComponentName(this, MusicListener::class.java)
        val sessions = try { manager.getActiveSessions(listener) } catch (_: SecurityException) { emptyList() }
        controller = sessions.firstOrNull { it.packageName in yandexPackages }
        val c = controller
        if (c == null) {
            status.text = "Нет доступной медиасессии. Включи музыку и выдай доступ к уведомлениям."
            title.text = "Музыка не запущена"; artist.text = "Яндекс Музыка"; art.setImageDrawable(null)
            play.text = "▶"; progress.progress = 0; return
        }
        status.text = "Подключено к Яндекс Музыке"
        val meta = c.metadata
        title.text = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Без названия"
        artist.text = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Неизвестный исполнитель"
        val bmp: Bitmap? = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (bmp != null) art.setImageBitmap(bmp) else art.setImageDrawable(null)
        val state = c.playbackState
        play.text = if (state?.state == PlaybackState.STATE_PLAYING) "⏸" else "▶"
        val length = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
        progress.max = length.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
        val pos = if (state != null && state.state == PlaybackState.STATE_PLAYING) {
            state.position + ((android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0) * state.playbackSpeed).toLong()
        } else state?.position ?: 0L
        if (!seeking) { progress.progress = pos.coerceIn(0, length.coerceAtLeast(0)).toInt(); elapsed.text = time(pos) }
        duration.text = time(length)
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
