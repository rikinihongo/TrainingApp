package com.sonpxp.trainingapp.japan

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Spanned
import android.text.style.ReplacementSpan

/**
 * Created by Sonpx on 10/24/2024
 * Copyright(©)Cloudxanh. All rights reserved.
 *
 * RubySpan xử lý việc hiển thị ruby text (furigana) phía trên text gốc.
 * Hỗ trợ căn chỉnh thông minh và xử lý các trường hợp text có độ dài khác nhau.
 */
class RubySpan private constructor(
    private val ruby: String,
    private val useColor: Boolean,
    private val color: Int,
    private val underline: Boolean
) : ReplacementSpan() {

    // Các metrics cho text và ruby
    private data class TextMetrics(
        val fontSize: Float = 0f,
        val textWidth: Float = 0f,
        val rubyWidth: Float = 0f,
        val newTextWidth: Float = 0f,
        val newRubyWidth: Float = 0f,
        val widthDiff: Float = 0f,
        val offsetX: Float = 0f,
        val measureSize: Float = 0f
    )

    // Trạng thái layout hiện tại
    private data class LayoutState(
        val relativeLength: RelativeLength = RelativeLength.UNKNOWN,
        val consecutiveRuby: Boolean = false,
        val enoughSpaceLeft: Boolean = false,
        val extraSkip: Float = 0f,
        val spaceLeft: Float = 0f
    )

    private var metrics = TextMetrics()
    private var layoutState = LayoutState()
    private var fontMetrics: Paint.FontMetrics? = null
    private var rubyLength: Int = 0

    // Các trạng thái độ dài tương đối có thể có
    enum class RelativeLength {
        UNKNOWN,    // Chưa xác định
        NORMAL,     // Không cần xử lý đặc biệt
        TOO_LONG,   // Ruby quá dài, text gốc cần kéo dãn
        TOO_SHORT   // Ruby quá ngắn, ruby text cần kéo dãn
    }

    /**
     * Tính toán metrics cho text và ruby dựa trên input
     */
    private fun calculateMetrics(paint: Paint, textLength: Int): TextMetrics {
        val fontSize = paint.textSize
        val textWidth = fontSize * textLength
        val rubyWidth = fontSize * FONT_SIZE_SCALE * rubyLength

        val (newTextWidth, newRubyWidth, widthDiff) = when {
            // Text và ruby có cùng số ký tự
            textLength == rubyLength -> Triple(
                textWidth,
                rubyWidth + (textWidth - FACTORL * fontSize - rubyWidth),
                textWidth - FACTORL * fontSize - rubyWidth
            )
            // Ruby quá dài
            textLength > 1 && rubyWidth > textWidth + FACTORS * fontSize -> {
                val diff = rubyWidth - (textWidth + FACTORS * fontSize)
                Triple(textWidth + diff, rubyWidth, diff)
            }
            // Ruby quá ngắn
            textLength > 1 && textLength < rubyLength && rubyWidth < textWidth -> {
                val diff = textWidth - FACTORS * fontSize - rubyWidth
                Triple(textWidth, textWidth - FACTORS * fontSize, diff)
            }
            // Trường hợp bình thường
            else -> Triple(textWidth, rubyWidth, 0f)
        }

        return TextMetrics(
            fontSize = fontSize,
            textWidth = textWidth,
            rubyWidth = rubyWidth,
            newTextWidth = newTextWidth,
            newRubyWidth = newRubyWidth,
            widthDiff = widthDiff,
            offsetX = (newTextWidth - newRubyWidth) / 2,
            measureSize = newTextWidth
        )
    }

    /**
     * Xử lý trường hợp có RubySpan liền kề
     */
    private fun handleConsecutiveRuby(
        text: Spanned,
        start: Int,
        measureSize: Float
    ): LayoutState {
        val objs = text.getSpans(0, start, this::class.java)
        if (objs.isEmpty() || text.getSpanEnd(objs.last()) != start) {
            return LayoutState()
        }

        val prevRubySpan = objs.last()
        val extraSkip = prevRubySpan.getExceededWidth() + maxOf(0f, -metrics.offsetX) + 5
        val prevSpaceLeft = prevRubySpan.getSpaceLeft()

        return LayoutState(
            relativeLength = determineRelativeLength(metrics),
            consecutiveRuby = true,
            enoughSpaceLeft = prevSpaceLeft > measureSize + extraSkip,
            extraSkip = extraSkip,
            spaceLeft = 0f // Will be set in draw()
        )
    }

    private fun determineRelativeLength(metrics: TextMetrics): RelativeLength = when {
        metrics.widthDiff == 0f -> RelativeLength.NORMAL
        metrics.newRubyWidth > metrics.newTextWidth -> RelativeLength.TOO_LONG
        else -> RelativeLength.TOO_SHORT
    }

    // Public APIs
    private fun getExceededWidth(): Float = maxOf(0f, metrics.newRubyWidth - metrics.newTextWidth)

    private fun getSpaceLeft(): Float = layoutState.spaceLeft

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        if (start >= end) return 0

        rubyLength = ruby.length
        fontMetrics = paint.fontMetrics
        metrics = calculateMetrics(paint, end - start)

        layoutState = when (text) {
            is Spanned -> handleConsecutiveRuby(text, start, metrics.measureSize)
            else -> LayoutState(relativeLength = determineRelativeLength(metrics))
        }

        return metrics.measureSize.toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        if (start >= end || ruby.isEmpty()) return

        val originalPaintState = paint.save()
        val bounds = Rect().also { canvas.getClipBounds(it) }

        layoutState = layoutState.copy(
            spaceLeft = bounds.right - x - metrics.measureSize
        )

        drawWithTransform(canvas, x, bounds) {
            drawText(canvas, text, start, end, x, y.toFloat(), paint)
            drawRuby(canvas, x, y.toFloat(), paint)
        }

        paint.restore(originalPaintState)
    }

    private inline fun drawWithTransform(
        canvas: Canvas,
        x: Float,
        bounds: Rect,
        drawOperations: () -> Unit
    ) {
        val needsTransform = x + metrics.offsetX >= bounds.left &&
                layoutState.consecutiveRuby &&
                layoutState.enoughSpaceLeft

        if (needsTransform) {
            canvas.save()
            canvas.translate(layoutState.extraSkip, 0f)
        }

        drawOperations()

        if (needsTransform) {
            canvas.restore()
        }
    }

    private fun Paint.save(): PaintState = PaintState(
        color = color,
        isUnderlineText = isUnderlineText,
        textSize = textSize
    )

    private fun Paint.restore(state: PaintState) {
        color = state.color
        isUnderlineText = state.isUnderlineText
        textSize = state.textSize
    }

    private data class PaintState(
        val color: Int,
        val isUnderlineText: Boolean,
        val textSize: Float
    )

    private fun drawText(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        if (useColor) paint.color = color
        paint.isUnderlineText = underline

        when (layoutState.relativeLength) {
            RelativeLength.TOO_LONG -> drawStretchedText(canvas, text, start, end, x, y, paint)
            else -> canvas.drawText(text, start, end, x, y, paint)
        }
    }

    private fun drawStretchedText(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val step = if (end - start > 1) {
            metrics.widthDiff / (end - start - 1) + metrics.fontSize
        } else 0f

        for (i in start until end) {
            canvas.drawText(
                /* text = */ text,
                /* start = */ i,
                /* end = */ i + 1,
                /* x = */ x + step * (i - start),
                /* y = */ y,
                /* paint = */ paint
            )
        }
    }

    private fun drawRuby(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        paint.apply {
            isUnderlineText = false
            textSize = metrics.fontSize * FONT_SIZE_SCALE
        }

        val offsetY = fontMetrics?.ascent?.times(1.1f) ?: 0f

        when (layoutState.relativeLength) {
            RelativeLength.TOO_SHORT -> drawStretchedRuby(canvas, x, y + offsetY, paint)
            else -> canvas.drawText(ruby, 0, rubyLength, x + metrics.offsetX, y + offsetY, paint)
        }
    }

    private fun drawStretchedRuby(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        val step = if (rubyLength > 1) {
            metrics.widthDiff / (rubyLength - 1) + metrics.rubyWidth / rubyLength
        } else 0f

        for (i in 0 until rubyLength) {
            canvas.drawText(
                /* text = */ ruby,
                /* start = */ i,
                /* end = */ i + 1,
                /* x = */ x + metrics.offsetX + step * i,
                /* y = */ y,
                /* paint = */ paint
            )
        }
    }

    companion object {
        private const val TAG = "RubySpan"
        private const val FONT_SIZE_SCALE = 0.5f
        private const val FACTORL = 0.5f
        private const val FACTORS = 0.2f

        // Factory methods
        @JvmStatic
        fun create(ruby: String) =
            RubySpan(ruby, false, 0, false)

        @JvmStatic
        fun createWithColor(ruby: String, color: Int) =
            RubySpan(ruby, true, color, false)

        @JvmStatic
        fun createWithUnderline(ruby: String) =
            RubySpan(ruby, false, 0, true)

        @JvmStatic
        fun createWithColorAndUnderline(ruby: String, color: Int) =
            RubySpan(ruby, true, color, true)
    }
}