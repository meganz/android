package mega.privacy.android.app.components.legacyfab

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.theme.AndroidTheme

abstract class LegacyFabButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    abstract val iconImageVector: ImageVector

    @get:DrawableRes
    abstract val iconResource: Int
    private val legacyView: FloatingActionButton?
    private val composeView: ComposeView?
    private var clickListener: () -> Unit = {}

    init {
        this.clipChildren = false
        this.clipToOutline = false
        this.clipToPadding = false
        val childView = if (useNewComponent) {
            legacyView = null
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AndroidTheme(isDark = isSystemInDarkTheme()) {
                        MegaFab(
                            onClick = { clickListener() },
                            painter = rememberVectorPainter(iconImageVector),
                        )
                    }
                }
                composeView = this
            }
        } else {
            composeView = null
            FloatingActionButton(context).apply {
                setImageResource(iconResource)
                legacyView = this
            }
        }
        addView(
            childView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
    }

    override fun setOnClickListener(l: OnClickListener?) {
        legacyView?.setOnClickListener(l)
        clickListener = { l?.onClick(composeView) }
    }

    fun show() {
        legacyView?.show()
        composeView?.visibility = VISIBLE
    }

    fun hide() {
        legacyView?.hide()
        composeView?.visibility = GONE
    }

    companion object {
        private var useNewComponent: Boolean = false

        fun useNewComponentsForLegacyFabButtons(value: Boolean) {
            useNewComponent = value
        }
    }
}