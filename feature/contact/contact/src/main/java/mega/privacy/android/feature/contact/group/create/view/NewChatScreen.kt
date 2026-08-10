package mega.privacy.android.feature.contact.group.create.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.add.view.ContactSelectionState
import mega.privacy.android.feature.contact.add.view.rememberContactSelectionState
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * New chat screen. A single mode-less MEGA-contacts multi-select picker used by the share/forward
 * target pickers ("new chat" entry points). The selection count decides the outcome, mirroring the
 * legacy `AddContactActivity` new-chat flow:
 *
 * - **Exactly one** contact selected → the FAB shows a check and confirms immediately as a 1:1 chat,
 *   skipping the group-settings step. [onConfirmOneToOne] is invoked with the single handle.
 * - **Two or more** contacts selected → the FAB shows a chevron and advances to the group-settings
 *   step (name, EKR, chat link, allow-add-participants); confirming there invokes [onConfirmGroup]
 *   with the selection and the chosen settings.
 *
 * Back from the settings step returns to selection. Selection is owned locally via
 * [rememberContactSelectionState] so it survives search/filter changes.
 *
 * @param state the searchable contacts state.
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onConfirmOneToOne invoked with the single selected handle for a 1:1 chat.
 * @param onConfirmGroup invoked with the selected handles and chosen settings for a group chat.
 * @param onBack invoked when the user navigates back out of the screen.
 * @param modifier
 * @param initialStep Selection or Settings
 */
@Composable
internal fun NewChatScreen(
    state: CreateChatUiState,
    onSearchQueryChange: (String?) -> Unit,
    onConfirmOneToOne: (selectedHandle: Long) -> Unit,
    onConfirmGroup: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
        imageUri: String?,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: NewChatStep = NewChatStep.Selection,
) {
    val selectionState = rememberContactSelectionState()
    var step by rememberSaveable { mutableStateOf(initialStep) }

    NewChatScreenContent(
        state = state,
        onSearchQueryChange = onSearchQueryChange,
        onConfirmOneToOne = onConfirmOneToOne,
        onConfirmGroup = onConfirmGroup,
        onBack = onBack,
        step = step,
        selectionState = selectionState,
        stepChange = { newStep -> step = newStep },
        modifier = modifier,
    )
}

@Composable
internal fun NewChatScreenContent(
    state: CreateChatUiState,
    onSearchQueryChange: (String?) -> Unit,
    onConfirmOneToOne: (selectedHandle: Long) -> Unit,
    onConfirmGroup: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
        imageUri: String?,
    ) -> Unit,
    onBack: () -> Unit,
    step: NewChatStep,
    selectionState: ContactSelectionState,
    stepChange: (NewChatStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = selectionState.selectedItemsCount
    when (step) {
        NewChatStep.Selection -> SelectionStep(
            state = state,
            selectedHandles = selectionState.selectedHandles,
            selectedCount = selectedCount,
            emptySelectionTitle = stringResource(sharedR.string.new_chat_title),
            fabIcon = if (selectedCount == 1) {
                rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
            } else {
                rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight)
            },
            tagPrefix = NEW_CHAT_TAG_PREFIX,
            showFab = selectedCount > 0,
            onSearchQueryChange = onSearchQueryChange,
            onToggle = selectionState::toggleSelection,
            onNext = {
                val handles = selectionState.selectedHandles
                if (handles.size == 1) {
                    onConfirmOneToOne(handles.first())
                } else {
                    stepChange(NewChatStep.Settings)
                }
            },
            onBack = onBack,
            modifier = modifier,
        )

        NewChatStep.Settings -> SettingsStep(
            contacts = (state as? CreateChatUiState.Data)?.contacts?.toImmutableSet()
                ?: emptySet<ContactItemUiState>().toImmutableSet(),
            selectedHandles = selectionState.selectedHandles.toImmutableSet(),
            selectedCount = selectedCount,
            tagPrefix = NEW_CHAT_TAG_PREFIX,
            onConfirm = { title, isEkr, isChatLink, allowAddParticipants, imageUri ->
                onConfirmGroup(
                    selectionState.selectedHandles,
                    title,
                    isEkr,
                    isChatLink,
                    allowAddParticipants,
                    imageUri,
                )
            },
            onRemoveParticipant = selectionState::toggleSelection,
            onBack = { stepChange(NewChatStep.Selection) },
            allowGroupImageSelection = (state as? CreateChatUiState.Data)?.allowGroupImageSelection == true,
            modifier = modifier,
        )
    }
}

internal enum class NewChatStep { Selection, Settings }

internal const val NEW_CHAT_TAG_PREFIX = "new_chat"

private class NewChatUiStateProvider : PreviewParameterProvider<CreateChatUiState> {
    override val values: Sequence<CreateChatUiState> = sequenceOf(
        CreateChatUiState.Loading,
        CreateChatUiState.Data(
            contacts = persistentListOf(),
            query = null,
            allowGroupImageSelection = false
        ),
        CreateChatUiState.Data(
            contacts = listOf(
                ContactItemUiState(
                    handle = 1L,
                    displayName = "Alice Anderson",
                    status = ContactItemStatus.Online,
                    lastSeen = null,
                    avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                    isVerified = true,
                    email = "alice@example.com",
                ),
                ContactItemUiState(
                    handle = 2L,
                    displayName = "Bob Brown",
                    status = ContactItemStatus.Away,
                    lastSeen = 45,
                    avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                    isVerified = false,
                    email = "bob@example.com",
                ),
            ).toImmutableList(),
            query = null,
            allowGroupImageSelection = false,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun NewChatScreenSelectionStepPreview(
    @PreviewParameter(NewChatUiStateProvider::class) state: CreateChatUiState,
) {
    AndroidThemeForPreviews {
        NewChatScreenContent(
            state = state,
            onSearchQueryChange = {},
            onConfirmOneToOne = {},
            onConfirmGroup = { _, _, _, _, _, _ -> },
            onBack = {},
            stepChange = {},
            step = NewChatStep.Selection,
            selectionState = rememberContactSelectionState(),
        )
    }
}
