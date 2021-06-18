package mega.privacy.android.app.textFileEditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import mega.privacy.android.app.textFileEditor.views.LineNumberViewUtils.addExtraOnDrawBehaviour
import mega.privacy.android.app.textFileEditor.views.LineNumberViewUtils.initTextPaint
import mega.privacy.android.app.textFileEditor.views.LineNumberViewUtils.updatePaddingsAndView

open class LineNumberTextView : AppCompatTextView {

    private val textPaint = Paint()

    private var lineNumberEnabled = false

    init {
        initTextPaint(textPaint)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        addExtraOnDrawBehaviour(lineNumberEnabled, canvas, textPaint)
        super.onDraw(canvas)
    }

    fun setLineNumberEnabled(lineNumberEnabled: Boolean) {
        this.lineNumberEnabled = lineNumberEnabled
        updatePaddingsAndView(lineNumberEnabled)
    }
}