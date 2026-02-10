package mega.privacy.android.app.components.legacyfab

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.icon.pack.R
import mega.privacy.android.icon.pack.IconPack

/**
 * A view component that can be used in classic XML layouts. It shows either:
 * - A classic [com.google.android.material.floatingactionbutton.FloatingActionButton] when [setUseNewComponentFromFeatureFlag] has been set to false
 * - A [androidx.compose.ui.platform.ComposeView] hosting [mega.android.core.ui.components.fab.MegaFab] when [setUseNewComponentFromFeatureFlag] has been set to true
 *
 * Set the click listener from code with [setOnClickListener].
 */
class LegacyFabButtonSend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LegacyFabButton(context, attrs, defStyleAttr) {
    override val iconImageVector: ImageVector = IconPack.Medium.Thin.Outline.SendHorizontal
    override val iconResource: Int get() = R.drawable.ic_send_horizontal_medium_thin_outline
}