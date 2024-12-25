package com.sonpxp.trainingapp.japan

import android.content.Context
import android.text.Spanned
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.sonpxp.trainingapp.R

/**
 * Created by Sonpx on 06/06/2024
 * Copyright(©)Cloudxanh. All rights reserved.
 */
/**
 * Custom TextView for displaying Japanese text with furigana (reading aids) above the main text.
 * Furigana is rendered using HTML ruby tags <ruby> and <rt>.
 */
class FuriganaTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var lineSpacing: Float = 0f

    companion object {
        private const val FURIGANA_SCALE = 0.5f
        private const val DEFAULT_LINE_HEIGHT_MULTIPLIER = 1.2f
        private const val STANDARD_LINE_HEIGHT_MULTIPLIER = 1f
    }

    init {
        initializeAttributes(attrs)
    }

    /**
     * Initializes custom attributes from XML layout
     * @param attrs AttributeSet from layout XML
     */
    private fun initializeAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.FuriganaTextView).apply {
                lineSpacing = getDimension(R.styleable.FuriganaTextView_lineSpacing, 0f)
                recycle()
            }
        }
    }

    /**
     * Sets text with furigana support
     * @param text HTML string containing ruby tags for furigana
     * @param validateFurigana if true, validates that the text contains proper furigana markup
     */
    fun setFuriganaText(text: String) {
        val spannableText = createSpannableText(text)
        applyFuriganaFormatting()
        setText(spannableText, BufferType.SPANNABLE)
    }

    /**
     * Creates spannable text from HTML string with ruby tag support
     * Handles different Android API levels for HTML parsing
     */
    private fun createSpannableText(text: String): Spanned {
        //return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT, null, RubyTagHandler())
        // Dùng LEGACY thay vì COMPACT để xử lý underline tốt hơn
        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT, null, RubyTagHandler())
    }

    /**
     * Applies formatting specific to furigana display:
     * - Adjusts line spacing for ruby text
     * - Adds top padding to prevent furigana from being cut off
     */
    private fun applyFuriganaFormatting() {
        setLineSpacing(lineSpacing, DEFAULT_LINE_HEIGHT_MULTIPLIER)
        // Calculate padding based on the furigana scale relative to main text size
        val furiganaPadding = (textSize * FURIGANA_SCALE).toInt()
        setPadding(paddingLeft, furiganaPadding, paddingRight, paddingBottom)
    }

    /**
     * Resets text formatting to standard display without furigana
     */
    private fun resetFormatting() {
        setLineSpacing(0f, STANDARD_LINE_HEIGHT_MULTIPLIER)
        setPadding(0, 0, 0, 0)
    }

    /**
     * Validates if the text contains proper furigana markup
     * Checks for properly nested ruby and rt tags
     */
    private fun isFuriganaValid(text: String): Boolean {
        val pattern = "(?:<[^>]*>)*<ruby>[^<]*<rt>[^<]*</rt></ruby>(?:<[^>]*>)*".toRegex()
        return pattern.matches(text)
    }

}