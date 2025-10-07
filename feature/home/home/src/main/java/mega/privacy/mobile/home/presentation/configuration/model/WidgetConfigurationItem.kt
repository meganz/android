package mega.privacy.mobile.home.presentation.configuration.model

import mega.android.core.ui.model.LocalizedText

data class WidgetConfigurationItem(
    val identifier: String,
    val index: Int,
    val name: LocalizedText,
    val enabled: Boolean,
    val canDelete: Boolean,
)
