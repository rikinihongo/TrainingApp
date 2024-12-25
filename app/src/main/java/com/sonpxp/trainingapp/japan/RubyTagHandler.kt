package com.sonpxp.trainingapp.japan

import android.text.Editable
import android.text.Html
import android.text.Spannable
import org.xml.sax.XMLReader

/**
 * Created by Sonpx on 10/24/2024
 * Copyright(©)Cloudxanh. All rights reserved.
 */
class RubyTagHandler : Html.TagHandler {

    private var rubyText: String = ""

    sealed class RubyBaseTag {
        data object RubyTag : RubyBaseTag()
        data object RbTag : RubyBaseTag()
        data object RtTag : RubyBaseTag()
        data object RpTag : RubyBaseTag()
    }

    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader
    ) {
        when {
            opening -> handleOpeningTag(tag.lowercase(), output)
            isRubyRelatedTag(tag.lowercase()) -> handleTagInternal(output)
        }
    }

    private fun handleOpeningTag(tag: String, output: Editable) {
        val markTag = when (tag) {
            "ruby" -> RubyBaseTag.RubyTag
            "rt" -> RubyBaseTag.RtTag
            "rb" -> RubyBaseTag.RbTag
            "rp" -> RubyBaseTag.RpTag
            else -> null
        }
        markTag?.let { output.start(it) }
    }

    private fun isRubyRelatedTag(tag: String): Boolean = tag in setOf("ruby", "rb", "rt", "rp")

    private fun handleTagInternal(output: Editable) {
        val obj = output.getLast<RubyBaseTag>() ?: return

        val where = output.getSpanStart(obj)
        val len = output.length

        output.removeSpan(obj)

        when (obj) {
            is RubyBaseTag.RpTag -> {
                // Ignore <rp> tags by removing their content
                output.delete(where, len)
            }

            is RubyBaseTag.RtTag -> {
                // Store ruby text and remove the RT content
                rubyText = output.subSequence(where, len).toString()
                output.delete(where, len)
            }

            is RubyBaseTag.RbTag -> {
                // No action needed for RbTag
            }

            is RubyBaseTag.RubyTag -> {
                // Apply ruby annotation if we have ruby text
                // RubySpan.createWithColor(rubyText, Color.parseColor("#3AC6C9"))
                if (rubyText.isNotEmpty()) {
                    output.setSpan(
                        /* p0 = */ RubySpan.create(rubyText),
                        /* p1 = */ where,
                        /* p2 = */ len,
                        /* p3 = */ Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // Reset ruby text after applying
                    rubyText = ""
                }
            }
        }
    }

    companion object {
        private inline fun <reified T : RubyBaseTag> Spannable.getLast(): T? {
            return getSpans(0, length, T::class.java).lastOrNull()
        }

        private fun Spannable.start(mark: RubyBaseTag) {
            setSpan(mark, length, length, Spannable.SPAN_MARK_MARK)
        }
    }

}