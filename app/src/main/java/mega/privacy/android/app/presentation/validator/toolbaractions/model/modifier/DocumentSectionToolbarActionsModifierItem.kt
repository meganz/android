package mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier

import androidx.compose.runtime.Immutable

@Immutable
data class DocumentSectionToolbarActionsModifierItem(
    val hiddenNodeItem: DocumentSectionHiddenNodeActionModifierItem = DocumentSectionHiddenNodeActionModifierItem(),
)

@Immutable
data class DocumentSectionHiddenNodeActionModifierItem(
    val isEnabled: Boolean = false,
    val canBeHidden: Boolean = false,
)
