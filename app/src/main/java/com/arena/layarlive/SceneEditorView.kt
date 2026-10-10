package com.arena.layarlive

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt

class SceneEditorView(context: Context) : FrameLayout(context) {
    enum class Layer { TEXT, SAWERIA }

    var scene: SceneConfig = SceneConfig()
        private set
    var selectedLayer: Layer = Layer.TEXT
        private set
    var onSceneChanged: (() -> Unit)? = null
    var onLayerSelected: ((Layer) -> Unit)? = null

    private val accent = Color.rgb(167, 139, 250)
    private val muted = Color.rgb(111, 122, 151)
    private val density = resources.displayMetrics.density

    init {
        clipChildren = true
        clipToPadding = true
        background = rounded(Color.rgb(8, 11, 20), Color.rgb(43, 52, 75), 20)
    }

    fun setScene(value: SceneConfig) {
        scene = value
        refresh()
    }

    fun selectLayer(layer: Layer) {
        selectedLayer = layer
        onLayerSelected?.invoke(layer)
        refresh()
    }

    fun refresh() {
        post {
            if (width > 0 && height > 0) renderLayers()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) renderLayers()
    }

    private fun renderLayers() {
        removeAllViews()
        addSafeGuides()
        if (scene.saweriaEnabled) addLayer(Layer.SAWERIA)
        if (scene.textEnabled) addLayer(Layer.TEXT)
    }

    private fun addSafeGuides() {
        val guide = View(context).apply {
            isClickable = false
            isFocusable = false
            background = rounded(Color.TRANSPARENT, Color.argb(75, 186, 197, 222), 14)
            alpha = 0.85f
        }
        val marginX = (width * 0.045f).roundToInt()
        val marginY = (height * 0.035f).roundToInt()
        addView(guide, LayoutParams(width - marginX * 2, height - marginY * 2).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = marginX
            topMargin = marginY
        })
        guide.elevation = 0f
    }

    private fun addLayer(layer: Layer) {
        val box = boxFor(layer)
        val selected = selectedLayer == layer
        val outer = FrameLayout(context).apply {
            tag = layer.name
            background = rounded(Color.TRANSPARENT, if (selected) accent else Color.argb(170, 139, 150, 177), 14)
            isClickable = true
            isFocusable = false
            elevation = if (selected) dp(5).toFloat() else dp(2).toFloat()
        }

        val body = TextView(context).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            isClickable = true
            isFocusable = false
        }
        if (layer == Layer.TEXT) {
            body.text = scene.text.ifBlank { "Teks overlay" }
            body.textSize = scene.textSizeSp.coerceIn(12f, 34f)
            body.setTextColor(scene.textColor)
            body.background = rounded(scene.textBackground, null, 12)
        } else {
            body.text = if (scene.saweriaUrl.isBlank()) {
                "SAWERIA ALERT\nTempel URL widget di bawah"
            } else {
                "SAWERIA ALERT\n{donator}  •  {amount}\nWidget aktif saat live"
            }
            body.textSize = 12f
            body.setTextColor(Color.rgb(230, 225, 255))
            body.background = rounded(Color.argb(225, 24, 21, 43), null, 12)
        }
        outer.addView(body, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        })

        val resizeHandle = TextView(context).apply {
            text = "↘"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(14, 13, 27))
            background = rounded(accent, null, 50)
            isClickable = true
            isFocusable = false
            contentDescription = "Geser untuk ubah ukuran"
        }
        outer.addView(resizeHandle, LayoutParams(dp(25), dp(25), Gravity.BOTTOM or Gravity.END).apply {
            rightMargin = dp(4)
            bottomMargin = dp(4)
        })

        val lp = LayoutParams(
            (width * box.width).roundToInt().coerceAtLeast(dp(58)),
            (height * box.height).roundToInt().coerceAtLeast(dp(38))
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = (width * box.x).roundToInt()
            topMargin = (height * box.y).roundToInt()
        }
        addView(outer, lp)

        body.setOnTouchListener { touchedView, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setSelectedVisual(outer, layer)
                    selectedLayer = layer
                    onLayerSelected?.invoke(layer)
                    beginGesture(layer, event, resizing = false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    moveLayer(layer, event, resizing = false)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    endGesture(touchedView, event)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
        resizeHandle.setOnTouchListener { touchedView, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setSelectedVisual(outer, layer)
                    selectedLayer = layer
                    onLayerSelected?.invoke(layer)
                    beginGesture(layer, event, resizing = true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    moveLayer(layer, event, resizing = true)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    endGesture(touchedView, event)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
    }

    private var startRawX = 0f
    private var startRawY = 0f
    private var startBox = LayerBox(0f, 0f, 0f, 0f)
    private var gestureIsResize = false

    private fun setSelectedVisual(selectedView: FrameLayout, layer: Layer) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.tag == Layer.TEXT.name || child.tag == Layer.SAWERIA.name) {
                val isSelected = child.tag == layer.name
                child.background = rounded(
                    Color.TRANSPARENT,
                    if (isSelected) accent else Color.argb(170, 139, 150, 177),
                    14
                )
                child.elevation = if (isSelected) dp(5).toFloat() else dp(2).toFloat()
            }
        }
        selectedView.contentDescription = if (layer == Layer.TEXT) "Text layer terpilih" else "Saweria layer terpilih"
    }

    private fun beginGesture(layer: Layer, event: MotionEvent, resizing: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(true)
        startRawX = event.rawX
        startRawY = event.rawY
        val box = boxFor(layer)
        startBox = LayerBox(box.x, box.y, box.width, box.height)
        gestureIsResize = resizing
    }

    private fun moveLayer(layer: Layer, event: MotionEvent, resizing: Boolean) {
        val box = boxFor(layer)
        val dx = (event.rawX - startRawX) / width.coerceAtLeast(1).toFloat()
        val dy = (event.rawY - startRawY) / height.coerceAtLeast(1).toFloat()
        if (resizing) {
            val maxWidth = (1f - box.x).coerceAtLeast(0.08f)
            val maxHeight = (1f - box.y).coerceAtLeast(0.08f)
            box.width = (startBox.width + dx).coerceIn(minOf(0.18f, maxWidth), maxWidth)
            box.height = (startBox.height + dy).coerceIn(minOf(0.075f, maxHeight), maxHeight)
        } else {
            box.x = (startBox.x + dx).coerceIn(0f, 1f - box.width)
            box.y = (startBox.y + dy).coerceIn(0f, 1f - box.height)
        }
        updateLayerLayout(layer, box)
    }

    private fun endGesture(view: View, event: MotionEvent) {
        parent?.requestDisallowInterceptTouchEvent(false)
        onSceneChanged?.invoke()
        if (!gestureIsResize && kotlin.math.abs(event.rawX - startRawX) < dp(6) &&
            kotlin.math.abs(event.rawY - startRawY) < dp(6)
        ) {
            view.performClick()
        }
    }

    private fun updateLayerLayout(layer: Layer, box: LayerBox) {
        val node = getLayerView(layer) ?: return
        val lp = node.layoutParams as LayoutParams
        lp.width = (width * box.width).roundToInt().coerceAtLeast(dp(58))
        lp.height = (height * box.height).roundToInt().coerceAtLeast(dp(38))
        lp.leftMargin = (width * box.x).roundToInt()
        lp.topMargin = (height * box.y).roundToInt()
        node.layoutParams = lp
    }

    private fun getLayerView(layer: Layer): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.tag == layer.name) return child
        }
        return null
    }

    private fun boxFor(layer: Layer): LayerBox = if (layer == Layer.TEXT) scene.textBox else scene.saweriaBox

    private fun rounded(fill: Int, stroke: Int?, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(fill)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()
}
