package com.arena.layarlive

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var scene: SceneConfig
    private lateinit var editor: SceneEditorView
    private lateinit var textInput: EditText
    private lateinit var saweriaInput: EditText
    private lateinit var permissionStatus: TextView
    private lateinit var textEnabledSwitch: Switch
    private lateinit var widgetEnabledSwitch: Switch
    private lateinit var layerTextButton: TextView
    private lateinit var layerSaweriaButton: TextView
    private var showWidgetUrl = false

    private val pageColor = Color.rgb(8, 11, 20)
    private val cardColor = Color.rgb(17, 23, 39)
    private val accentColor = Color.rgb(167, 139, 250)
    private val mutedColor = Color.rgb(160, 170, 191)
    private val primaryText = Color.rgb(245, 247, 255)
    private val outlineColor = Color.rgb(43, 53, 76)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scene = SceneStore.load(this)
        window.statusBarColor = pageColor
        window.navigationBarColor = pageColor
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionStatus.isInitialized) refreshPermissionStatus()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(32))
            setBackgroundColor(pageColor)
        }

        page.addView(makeText("LAYARLIVE  /  SCENE EDITOR", 11f, accentColor, true))
        addGap(page, 7)
        page.addView(makeText("Atur overlay.\nFokus tetap ke game.", 27f, primaryText, true))
        addGap(page, 7)
        page.addView(makeText(
            "Susun layer di preview, kunci touch-through, lalu mulai TikTok LIVE screen-share.",
            14f,
            mutedColor,
            false
        ))
        addGap(page, 18)

        val stageCard = card()
        val stageHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val stageHeading = makeText("PREVIEW SIARAN", 13f, primaryText, true)
        stageHeader.addView(stageHeading, LinearLayout.LayoutParams(0, dp(24), 1f))
        stageHeader.addView(pill("DRAG • RESIZE", Color.rgb(45, 37, 78), accentColor))
        stageCard.addView(stageHeader)
        addGap(stageCard, 8)
        stageCard.addView(makeText("Seret layer untuk pindah; tarik sudut ↘ untuk ubah ukuran.", 12f, mutedColor, false))
        addGap(stageCard, 13)

        val ratio = resources.displayMetrics.widthPixels.toFloat() /
            resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)
        val stageHeight = dp(470)
        val stageWidth = (stageHeight * ratio).toInt().coerceIn(dp(190), dp(270))
        editor = SceneEditorView(this)
        editor.setScene(scene)
        val stageFrame = FrameLayout(this).apply {
            addView(editor, FrameLayout.LayoutParams(stageWidth, stageHeight, Gravity.CENTER))
        }
        stageCard.addView(stageFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, stageHeight))
        addGap(stageCard, 12)

        val layerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        layerTextButton = makeButton("01  TEXT LABEL", Color.rgb(53, 43, 91), Color.rgb(230, 222, 255))
        layerSaweriaButton = makeButton("02  SAWERIA", Color.rgb(29, 36, 54), mutedColor)
        layerTextButton.setOnClickListener { editor.selectLayer(SceneEditorView.Layer.TEXT) }
        layerSaweriaButton.setOnClickListener { editor.selectLayer(SceneEditorView.Layer.SAWERIA) }
        layerRow.addView(layerTextButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        layerRow.addView(layerSaweriaButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(5) })
        stageCard.addView(layerRow)
        page.addView(stageCard, matchWrap())
        addGap(page, 14)

        val textCard = card()
        val textHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textHeading = makeText("TEXT LABEL", 14f, primaryText, true)
        textHeader.addView(textHeading, LinearLayout.LayoutParams(0, dp(32), 1f))
        textEnabledSwitch = makeSwitch("Tampil", scene.textEnabled)
        textHeader.addView(textEnabledSwitch)
        textCard.addView(textHeader)
        addGap(textCard, 10)
        textCard.addView(makeText("Tulis label singkat yang nyaman dilihat saat bermain.", 12f, mutedColor, false))
        addGap(textCard, 8)
        textInput = makeInput(scene.text, "Contoh: LIVE • MAKASIH UDAH MAMPIR!", false)
        textCard.addView(textInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)))
        addGap(textCard, 13)

        val sizeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sizeRow.addView(makeText("UKURAN TEKS", 11f, mutedColor, true), LinearLayout.LayoutParams(0, dp(26), 1f))
        val sizeValue = makeText("${scene.textSizeSp.toInt()} sp", 11f, accentColor, true)
        sizeRow.addView(sizeValue)
        textCard.addView(sizeRow)
        val sizeSeek = SeekBar(this).apply {
            max = 26
            progress = (scene.textSizeSp.toInt() - 12).coerceIn(0, max)
            progressTintList = ColorStateList.valueOf(accentColor)
            thumbTintList = ColorStateList.valueOf(accentColor)
        }
        textCard.addView(sizeSeek, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)))
        addGap(textCard, 6)
        textCard.addView(makeText("WARNA", 11f, mutedColor, true))
        addGap(textCard, 8)
        val colorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val colorOptions = listOf(
            ColorOption("Putih", Color.WHITE, 0xD9161D30.toInt()),
            ColorOption("Lavender", Color.rgb(230, 222, 255), 0xDD2A2147.toInt()),
            ColorOption("Mint", Color.rgb(186, 255, 226), 0xDD12382F.toInt()),
            ColorOption("Gold", Color.rgb(255, 228, 176), 0xDD392A14.toInt())
        )
        colorOptions.forEachIndexed { index, option ->
            val chip = makeButton(option.name, option.background, option.textColor)
            chip.textSize = 10f
            chip.setOnClickListener {
                scene.textColor = option.textColor
                scene.textBackground = option.background
                saveSceneAndRefresh()
                updateColorChipStyles(colorRow, colorOptions, index)
            }
            colorRow.addView(chip, LinearLayout.LayoutParams(0, dp(37), 1f).apply {
                if (index > 0) marginStart = dp(4)
                if (index < colorOptions.lastIndex) marginEnd = dp(4)
            })
        }
        textCard.addView(colorRow)
        updateColorChipStyles(colorRow, colorOptions, colorOptions.indexOfFirst {
            it.textColor == scene.textColor && it.background == scene.textBackground
        }.coerceAtLeast(0))
        page.addView(textCard, matchWrap())
        addGap(page, 14)

        val widgetCard = card()
        val widgetHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val widgetHeading = makeText("SAWERIA ALERT WIDGET", 14f, primaryText, true)
        widgetHeader.addView(widgetHeading, LinearLayout.LayoutParams(0, dp(32), 1f))
        widgetEnabledSwitch = makeSwitch("Tampil", scene.saweriaEnabled)
        widgetHeader.addView(widgetEnabledSwitch)
        widgetCard.addView(widgetHeader)
        addGap(widgetCard, 6)
        widgetCard.addView(makeText(
            "Tempel URL Alert dari dashboard Saweria. URL disimpan terenkripsi di HP dan tidak dikirim ke server LayarLive.",
            12f,
            mutedColor,
            false
        ))
        addGap(widgetCard, 10)

        val urlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        saweriaInput = makeInput(scene.saweriaUrl, "https://saweria.co/widgets/alert?...", true)
        saweriaInput.transformationMethod = PasswordTransformationMethod.getInstance()
        urlRow.addView(saweriaInput, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginEnd = dp(7) })
        val revealButton = makeButton("LIHAT", Color.rgb(39, 46, 65), primaryText)
        revealButton.textSize = 10f
        revealButton.setOnClickListener {
            showWidgetUrl = !showWidgetUrl
            saweriaInput.transformationMethod = if (showWidgetUrl) null else PasswordTransformationMethod.getInstance()
            saweriaInput.setSelection(saweriaInput.text.length)
            revealButton.text = if (showWidgetUrl) "TUTUP" else "LIHAT"
        }
        urlRow.addView(revealButton, LinearLayout.LayoutParams(dp(68), dp(48)))
        widgetCard.addView(urlRow)
        addGap(widgetCard, 8)

        val widgetActionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val saveUrlButton = makeButton("SIMPAN URL", Color.rgb(54, 43, 91), Color.rgb(230, 222, 255))
        val clearUrlButton = makeButton("HAPUS", Color.rgb(40, 44, 58), mutedColor)
        widgetActionRow.addView(saveUrlButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        widgetActionRow.addView(clearUrlButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(5) })
        widgetCard.addView(widgetActionRow)
        addGap(widgetCard, 8)
        widgetCard.addView(makeText("Drag layer Saweria di preview untuk memilih posisi dan ukuran.", 11f, mutedColor, false))
        page.addView(widgetCard, matchWrap())
        addGap(page, 14)

        val liveCard = card()
        liveCard.addView(makeText("MODE SAAT LIVE", 13f, primaryText, true))
        addGap(liveCard, 8)
        liveCard.addView(makeText(
            "Overlay terkunci dan click-through: elemen tampil, tetapi sentuhan game diteruskan ke game. Geser dan ubah ukuran di editor sebelum LIVE.",
            12f,
            mutedColor,
            false
        ))
        addGap(liveCard, 12)
        permissionStatus = makeText("Memeriksa izin overlay…", 12f, mutedColor, true)
        liveCard.addView(permissionStatus)
        page.addView(liveCard, matchWrap())
        addGap(page, 12)

        val startButton = makeButton("AKTIFKAN OVERLAY  ·  KUNCI TOUCH-THROUGH", accentColor, Color.rgb(16, 12, 29))
        startButton.textSize = 12f
        startButton.setOnClickListener { startSceneOverlay() }
        page.addView(startButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))
        addGap(page, 8)
        val stopButton = makeButton("MATIKAN OVERLAY", Color.rgb(39, 30, 45), Color.rgb(255, 190, 205))
        stopButton.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Overlay dimatikan", Toast.LENGTH_SHORT).show()
        }
        page.addView(stopButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)))
        addGap(page, 16)
        page.addView(makeText(
            "Catatan: Saweria widget perlu koneksi internet dan diuji sebelum LIVE. Alert otomatis follow/gift TikTok dan mixer audio belum termasuk versi ini.",
            11f,
            mutedColor,
            false
        ))

        editor.onSceneChanged = { SceneStore.save(this, scene) }
        editor.onLayerSelected = { updateLayerChipStyles() }
        textEnabledSwitch.setOnCheckedChangeListener { _, checked ->
            scene.textEnabled = checked
            saveSceneAndRefresh()
        }
        widgetEnabledSwitch.setOnCheckedChangeListener { _, checked ->
            scene.saweriaEnabled = checked
            saveSceneAndRefresh()
        }
        textInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                scene.text = s?.toString().orEmpty()
                saveSceneAndRefresh()
            }
        })
        sizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scene.textSizeSp = (progress + 12).toFloat()
                sizeValue.text = "${scene.textSizeSp.toInt()} sp"
                saveSceneAndRefresh()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        saveUrlButton.setOnClickListener { saveWidgetUrl() }
        clearUrlButton.setOnClickListener {
            saweriaInput.setText("")
            scene.saweriaUrl = ""
            scene.saweriaEnabled = false
            widgetEnabledSwitch.isChecked = false
            saveSceneAndRefresh()
            Toast.makeText(this, "URL Saweria dihapus dari HP", Toast.LENGTH_SHORT).show()
        }
        updateLayerChipStyles()
        refreshPermissionStatus()

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(pageColor)
            addView(page)
        }
        setContentView(scroll)
    }

    private fun startSceneOverlay() {
        if (scene.saweriaEnabled && !isValidSaweriaUrl(scene.saweriaUrl)) {
            Toast.makeText(this, "Simpan URL Alert Saweria yang valid, atau matikan layer Saweria.", Toast.LENGTH_LONG).show()
            return
        }
        SceneStore.save(this, scene)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan LayarLive tampil di atas aplikasi lain, lalu tekan tombol lagi.", Toast.LENGTH_LONG).show()
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_SCENE
        }
        startForegroundService(serviceIntent)
        Toast.makeText(this, "Overlay aktif dan touch-through terkunci", Toast.LENGTH_SHORT).show()
    }

    private fun saveWidgetUrl() {
        val candidate = saweriaInput.text.toString().trim()
        if (!isValidSaweriaUrl(candidate)) {
            Toast.makeText(this, "Gunakan URL HTTPS Saweria Alert dengan streamKey.", Toast.LENGTH_LONG).show()
            return
        }
        scene.saweriaUrl = candidate
        scene.saweriaEnabled = true
        widgetEnabledSwitch.isChecked = true
        saveSceneAndRefresh()
        Toast.makeText(this, "URL tersimpan terenkripsi di HP", Toast.LENGTH_SHORT).show()
    }

    private fun isValidSaweriaUrl(value: String): Boolean = try {
        val uri = Uri.parse(value)
        val allowedHost = uri.host == "saweria.co" || uri.host == "www.saweria.co"
        uri.scheme == "https" && allowedHost &&
            uri.path.orEmpty().startsWith("/widgets/alert") &&
            !uri.getQueryParameter("streamKey").isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    private fun saveSceneAndRefresh() {
        SceneStore.save(this, scene)
        if (::editor.isInitialized) editor.refresh()
    }

    private fun updateLayerChipStyles() {
        if (!::layerTextButton.isInitialized || !::layerSaweriaButton.isInitialized || !::editor.isInitialized) return
        val textSelected = editor.selectedLayer == SceneEditorView.Layer.TEXT
        layerTextButton.background = rounded(
            if (textSelected) Color.rgb(53, 43, 91) else Color.rgb(29, 36, 54),
            if (textSelected) accentColor else null,
            14
        )
        layerTextButton.setTextColor(if (textSelected) Color.rgb(230, 222, 255) else mutedColor)
        layerSaweriaButton.background = rounded(
            if (!textSelected) Color.rgb(53, 43, 91) else Color.rgb(29, 36, 54),
            if (!textSelected) accentColor else null,
            14
        )
        layerSaweriaButton.setTextColor(if (!textSelected) Color.rgb(230, 222, 255) else mutedColor)
    }

    private fun updateColorChipStyles(row: LinearLayout, options: List<ColorOption>, selectedIndex: Int) {
        for (index in 0 until row.childCount) {
            val chip = row.getChildAt(index) as? TextView ?: continue
            val option = options[index]
            chip.background = rounded(
                option.background,
                if (index == selectedIndex) accentColor else null,
                12
            )
        }
    }

    private fun refreshPermissionStatus() {
        val hasPermission = Settings.canDrawOverlays(this)
        permissionStatus.text = if (hasPermission) "Izin overlay aktif  ✓" else "Izin overlay belum aktif"
        permissionStatus.setTextColor(if (hasPermission) Color.rgb(110, 231, 183) else Color.rgb(255, 190, 110))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        background = rounded(cardColor, outlineColor, 18)
    }

    private fun makeInput(value: String, hintValue: String, uriInput: Boolean): EditText = EditText(this).apply {
        setSingleLine(true)
        textSize = 14f
        setTextColor(primaryText)
        setHintTextColor(Color.rgb(116, 126, 149))
        hint = hintValue
        setPadding(dp(12), 0, dp(12), 0)
        background = rounded(Color.rgb(10, 14, 25), Color.rgb(53, 62, 83), 13)
        inputType = if (uriInput) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI else InputType.TYPE_CLASS_TEXT
        setText(value)
    }

    private fun makeSwitch(label: String, checked: Boolean): Switch = Switch(this).apply {
        text = label
        textSize = 11f
        setTextColor(mutedColor)
        isChecked = checked
        thumbTintList = ColorStateList.valueOf(accentColor)
        trackTintList = ColorStateList.valueOf(Color.rgb(70, 60, 104))
    }

    private fun makeText(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun makeButton(label: String, fill: Int, text: Int): TextView = TextView(this).apply {
        this.text = label
        textSize = 12f
        setTextColor(text)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(7), dp(9), dp(7))
        background = rounded(fill, null, 14)
        isClickable = true
        isFocusable = true
    }

    private fun pill(label: String, fill: Int, text: Int): TextView = TextView(this).apply {
        this.text = label
        textSize = 9f
        setTextColor(text)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(5), dp(8), dp(5))
        background = rounded(fill, null, 50)
    }

    private fun rounded(fill: Int, stroke: Int?, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(fill)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun matchWrap(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun addGap(parent: LinearLayout, heightDp: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(heightDp)))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class ColorOption(val name: String, val textColor: Int, val background: Int)

    companion object {
        private const val REQUEST_NOTIFICATIONS = 410
    }
}
