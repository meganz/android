package mega.privacy.android.app.textFileEditor.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import mega.privacy.android.app.R
import mega.privacy.android.app.textFileEditor.Pagination.Companion.LINE_BREAK
import mega.privacy.android.app.utils.ColorUtils

object LineNumberViewUtils {

    /**
     * Initializes the paint to draw line numbers if needed.
     *
     * @param paint Text paint to initialize.
     */
    fun View.initTextPaint(paint: Paint) {
        paint.apply {
            style = Paint.Style.FILL
            color = ColorUtils.getThemeColor(context, android.R.attr.textColorPrimary)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = resources.getDimensionPixelSize(R.dimen.line_number_size).toFloat()
        }
    }

    /**
     * Draws line numbers if are enabled.
     *
     * @param lineNumberEnabled True if line numbers are enabled, false otherwise.
     * @param firstLineNumber   Number to show as first line number.
     * @param canvas            Canvas in which line numbers have to be drawn.
     * @param paint             Text paint with which line numbers have to be painted.
     */
    fun TextView.addExtraOnDrawBehaviour(
        lineNumberEnabled: Boolean,
        firstLineNumber: Int,
        canvas: Canvas,
        paint: Paint
    ) {
        if (lineNumberEnabled) {
            val padding = resources.getDimensionPixelSize(R.dimen.line_number_padding)
            var lineNumber = firstLineNumber
            var endOfLine = true

            for (i in 0 until lineCount) {
                val textNumber = lineNumber.toString()
                val lineNumberStringLength = paint.measureText(textNumber)
                val start = paddingStart - lineNumberStringLength - padding
                val lineNumberRect = Rect()
                paint.getTextBounds(textNumber, 0, textNumber.length, lineNumberRect)
                val top = layout.getLineBottom(i) + (lineNumberRect.height() / 2)

                if (endOfLine) {
                    canvas.drawText("$lineNumber", start, top.toFloat(), paint)
                    lineNumber++
                    endOfLine = false
                }

                if (text.substring(layout.getLineStart(i), layout.getLineEnd(i)).endsWith(LINE_BREAK)) {
                    endOfLine = true
                }
            }
        }
    }

    /**
     * Updates paddings depending on if line numbers are enabled or not.
     * Invalidates the view to force redraw and show/hide line numbers.
     *
     * @param lineNumberEnabled True if line numbers are enabled, false otherwise.
     */
    fun TextView.updatePaddingsAndView(lineNumberEnabled: Boolean) {
        val start = resources.getDimensionPixelSize(
            if (lineNumberEnabled) R.dimen.text_editor_padding_start_with_nLines
            else R.dimen.text_editor_padding_without_nLines
        )

        val end = resources.getDimensionPixelSize(
            if (lineNumberEnabled) R.dimen.text_editor_padding_end_with_nLines
            else R.dimen.text_editor_padding_without_nLines
        )

        setPadding(start, paddingTop, end, paddingBottom)
        invalidate()
    }
}