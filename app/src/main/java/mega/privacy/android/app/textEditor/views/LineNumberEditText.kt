package mega.privacy.android.app.textEditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.addExtraOnDrawBehaviour
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.initTextPaint
import mega.privacy.android.app.textEditor.views.LineNumberViewUtils.updatePaddingsAndView
import mega.privacy.android.app.utils.StringResourcesUtils.getString


class LineNumberEditText : AppCompatEditText {

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
        setText(text)
    }

    /**
     * Rename this in this way to avoid enter any type of image in the text editor and redirect
     * the user to the import screen.
     */
    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(editorInfo)
        if (inputConnection == null) return inputConnection

        EditorInfoCompat.setContentMimeTypes(
                editorInfo,
                arrayOf("image/*", "image/png", "image/gif", "image/jpeg")
        )

        @Suppress("DEPRECATION")
        return InputConnectionCompat.createWrapper(
                inputConnection,
                editorInfo
        ) { _: InputContentInfoCompat?, _: Int, _: Bundle? ->
            Toast.makeText(
                    context,
                    getString(R.string.image_insertion_not_allowed),
                    Toast.LENGTH_SHORT
            ).show()
            true
        }
    }
}