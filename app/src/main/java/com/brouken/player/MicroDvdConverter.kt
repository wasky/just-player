@file:Suppress("OPT_IN_USAGE")

package com.brouken.player

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class MicroDvdConverter(context: Context, videoUri: Uri) {

    private val lineRegex = Regex("^\\{(-?\\d+)\\}\\{(-?\\d+)\\}(.*)$")
    private val styleRegex = Regex("\\{y:([^}]*)\\}", RegexOption.IGNORE_CASE)
    private val colorRegex = Regex("\\{c:\\$([0-9a-fA-F]{6})\\}", RegexOption.IGNORE_CASE)
    private val otherTagRegex = Regex("\\{[A-Za-z]:[^}]*\\}")
    private val frameRateRegex = Regex("\\d+(\\.\\d+)?")

    private var cueIndex = 1

    private val frameRate: Double by lazy {
        val frameRate = Utils.getFrameRate(context, videoUri)
        if (frameRate > 0) {
            frameRate.toDouble()
        } else {
            val msg = "Couldn't read frame rate. Using default value: $DEFAULT_FRAME_RATE"
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            DEFAULT_FRAME_RATE
        }
    }

    fun isMicroDvdLine(line: String): Boolean {
        val normalized = normalizeLinePrefix(line)
        return lineRegex.matches(normalized)
    }

    fun convertMicroDvdLineToSrt(line: String): String {
        val normalized = normalizeLinePrefix(line)
        val match = lineRegex.find(normalized) ?: return line
        val startFrame = match.groupValues[1].toLongOrNull() ?: return line
        val endFrame = match.groupValues[2].toLongOrNull() ?: return line
        val rawText = match.groupValues[3]

        if (isFrameRateHeader(startFrame, endFrame, rawText)) {
            return ""
        }

        val startTime = formatTime(startFrame)
        val endTime = formatTime(endFrame)
        val text = convertText(rawText)
        val index = cueIndex++

        return buildString {
            append(index).append('\n')
            append(startTime).append(" --> ").append(endTime).append('\n')
            append(text).append('\n')
        }
    }

    private fun normalizeLinePrefix(line: String): String {
        return line.removePrefix("\uFEFF").trimStart()
    }

    private fun isFrameRateHeader(startFrame: Long, endFrame: Long, text: String): Boolean {
        if (startFrame != endFrame || startFrame > 1L) return false
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.matches(frameRateRegex)
    }

    private fun formatTime(frame: Long): String {
        val safeFrame = frame.coerceAtLeast(0)
        val totalMillis = ((safeFrame.toDouble() / frameRate) * 1000.0).roundToLong()

        val hours = totalMillis / 3_600_000
        val minutes = (totalMillis % 3_600_000) / 60_000
        val seconds = (totalMillis % 60_000) / 1_000
        val millis = totalMillis % 1_000

        return buildString(12) {
            if (hours < 10) append('0')
            append(hours)
            append(':')

            if (minutes < 10) append('0')
            append(minutes)
            append(':')

            if (seconds < 10) append('0')
            append(seconds)
            append(',')

            if (millis < 100) append('0')
            if (millis < 10) append('0')
            append(millis)
        }
    }

    private fun convertText(text: String): String {
        val (cleanText, style) = extractStyles(text)
        val withBreaks = convertLineBreaks(cleanText)
        val withLineItalics = applyLineItalics(withBreaks, style.italic)

        var result = withLineItalics
        if (style.italic) result = "<i>$result</i>"
        if (style.bold) result = "<b>$result</b>"
        if (style.underline) result = "<u>$result</u>"
        if (style.color != null) result = "<font color=\"${style.color}\">$result</font>"

        return result
    }

    private fun convertLineBreaks(text: String): String {
        val placeholder = '\u0000'
        return text
            .replace("\\|", placeholder.toString())
            .replace("|", "\n")
            .replace(placeholder.toString(), "|")
    }

    private fun applyLineItalics(text: String, globalItalic: Boolean): String {
        val lines = text.split('\n')
        if (lines.size == 1) {
            return applyLineItalic(lines[0], globalItalic)
        }
        return lines.joinToString("\n") { applyLineItalic(it, globalItalic) }
    }

    private fun applyLineItalic(line: String, globalItalic: Boolean): String {
        if (!line.startsWith("/")) return line
        val withoutSlash = line.substring(1)
        if (globalItalic || withoutSlash.isEmpty()) return withoutSlash
        return "<i>$withoutSlash</i>"
    }

    private fun extractStyles(text: String): Pair<String, StyleFlags> {
        val style = StyleFlags()
        var result = styleRegex.replace(text) { match ->
            val styles = match.groupValues[1]
            for (ch in styles) {
                when (ch.lowercaseChar()) {
                    'i' -> style.italic = true
                    'b' -> style.bold = true
                    'u' -> style.underline = true
                }
            }
            ""
        }

        result = colorRegex.replace(result) { match ->
            if (style.color == null) {
                style.color = "#" + match.groupValues[1]
            }
            ""
        }

        result = otherTagRegex.replace(result, "")
        return result to style
    }

    private data class StyleFlags(
        var bold: Boolean = false,
        var italic: Boolean = false,
        var underline: Boolean = false,
        var color: String? = null
    )

    companion object {
        private const val DEFAULT_FRAME_RATE = 24000.0 / 1001.0 // 23.976023976023978
    }

}