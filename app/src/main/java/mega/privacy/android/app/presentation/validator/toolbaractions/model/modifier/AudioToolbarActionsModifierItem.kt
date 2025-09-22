package mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier

import androidx.compose.runtime.Immutable

@Immutable
data class AudioToolbarActionsModifierItem(
    val hiddenNodeItem: AudioHiddenNodeActionModifierItem = AudioHiddenNodeActionModifierItem(),
)

@Immutable
data class AudioHiddenNodeActionModifierItem(
    val isEnabled: Boolean = false,
    val canBeHidden: Boolean = false,
)
