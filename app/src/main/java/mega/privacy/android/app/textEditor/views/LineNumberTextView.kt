package mega.privacy.android.app.textEditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.addExtraOnDrawBehaviour
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.initTextPaint
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.updatePaddingsAndView

open class LineNumberTextView : AppCompatTextView {

    private val textPaint = Paint()

    private var lineNumberEnabled = false
    private var firstLineNumber = 1

    init {
        initTextPaint(textPaint)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        addExtraOnDrawBehaviour(lineNumberEnabled, firstLineNumber, canvas, textPaint)
        super.onDraw(canvas)
    }

    /**
     * Enables or disabled the behaviour to show line numbers.
     *
     * @param lineNumberEnabled True if should show line numbers, false otherwise.
     */
    fun setLineNumberEnabled(lineNumberEnabled: Boolean) {
        this.lineNumberEnabled = lineNumberEnabled
        updatePaddingsAndView(lineNumberEnabled)
    }

    /**
     * Sets the text to be displayed and updates the value to show as first line number.
     *
     * @param text            Text to be displayed.
     * @param firstLineNumber Number to show as first line number.
     */
    fun setText(text: CharSequence?, firstLineNumber: Int) {
        this.firstLineNumber = firstLineNumber
        this.text = text
    }
}