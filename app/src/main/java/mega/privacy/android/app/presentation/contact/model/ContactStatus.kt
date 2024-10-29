package mega.privacy.android.app.presentation.contact.model

import androidx.compose.ui.graphics.painter.Painter

/**
 * Contact status
 *
 * @property iconPainter
 * @property statusText
 */
data class ContactStatus(
    val iconPainter: Painter?,
    val statusText: String?,
)
