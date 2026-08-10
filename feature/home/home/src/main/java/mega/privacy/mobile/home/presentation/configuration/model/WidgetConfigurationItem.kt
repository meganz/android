package mega.privacy.mobile.home.presentation.configuration.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

@Immutable
data class WidgetConfigurationItem(
    val identifier: String,
    val index: Int,
    val name: LocalizedText,
    val enabled: Boolean,
    val canDelete: Boolean,
    val isConfigurable: Boolean,
    val isDraggable: Boolean,
)
