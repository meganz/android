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
 * Create group chat screen. A distinct two-step screen built on the shared contacts multi-select picker:
 *
 * 1. **Selection** — search and toggle MEGA contacts (selection owned locally via
 *    [rememberContactSelectionState] so it survives search/filter changes).
 * 2. **Settings** — group name (optional), encryption key rotation (EKR) toggle, get-chat-link toggle and
 *    allow-non-hosts-to-add-participants toggle.
 *
 * Enabling EKR hides the chat-link toggle (a chat link cannot be created for an EKR chat), matching the
 * legacy `AddContactActivity` group mode. Allow-add-participants defaults to on. On confirm the screen
 * reports the selection plus settings via [onConfirm]; it does not create the group itself.
 *
 * @param state
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onConfirm invoked with the selected contact handles and the chosen group settings.
 * @param onBack invoked when the user navigates back out of the screen.
 * @param modifier
 * @param initialStep Selection or Settings
 */
@Composable
internal fun CreateGroupChatScreen(
    state: CreateChatUiState,
    allowEmptyGroup: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
        imageUri: String?,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: CreateGroupChatStep = CreateGroupChatStep.Selection,
) {
    val selectionState = rememberContactSelectionState()
    var step by rememberSaveable { mutableStateOf(initialStep) }

    CreateGroupChatScreenContent(
        state = state,
        allowEmptyGroup = allowEmptyGroup,
        onSearchQueryChange = onSearchQueryChange,
        onConfirm = onConfirm,
        onBack = onBack,
        step = step,
        selectionState = selectionState,
        stepChange = { newStep -> step = newStep },
        modifier = modifier,
    )
}

@Composable
internal fun CreateGroupChatScreenContent(
    state: CreateChatUiState,
    allowEmptyGroup: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
        imageUri: String?,
    ) -> Unit,
    onBack: () -> Unit,
    step: CreateGroupChatStep,
    selectionState: ContactSelectionState,
    stepChange: (CreateGroupChatStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (step) {
        CreateGroupChatStep.Selection -> SelectionStep(
            state = state,
            selectedHandles = selectionState.selectedHandles,
            selectedCount = selectionState.selectedItemsCount,
            emptySelectionTitle = stringResource(sharedR.string.general_new_group_chat),
            fabIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
            tagPrefix = CREATE_GROUP_CHAT_TAG_PREFIX,
            showFab = selectionState.selectedItemsCount > 0 || allowEmptyGroup,
            onSearchQueryChange = onSearchQueryChange,
            onToggle = selectionState::toggleSelection,
            onNext = {
                stepChange(CreateGroupChatStep.Settings)
            },
            onBack = onBack,
            modifier = modifier,
        )

        CreateGroupChatStep.Settings -> SettingsStep(
            contacts = (state as? CreateChatUiState.Data)?.contacts?.toImmutableSet()
                ?: emptyList<ContactItemUiState>().toImmutableSet(),
            selectedHandles = selectionState.selectedHandles.toImmutableSet(),
            selectedCount = selectionState.selectedItemsCount,
            tagPrefix = CREATE_GROUP_CHAT_TAG_PREFIX,
            onConfirm = { title, isEkr, isChatLink, allowAddParticipants, imageUri ->
                onConfirm(
                    selectionState.selectedHandles,
                    title,
                    isEkr,
                    isChatLink,
                    allowAddParticipants,
                    imageUri,
                )
            },
            onRemoveParticipant = selectionState::toggleSelection,
            onBack = { stepChange(CreateGroupChatStep.Selection) },
            allowGroupImageSelection = (state as? CreateChatUiState.Data)?.allowGroupImageSelection == true,
            modifier = modifier,
        )
    }
}

internal enum class CreateGroupChatStep { Selection, Settings }

internal const val CREATE_GROUP_CHAT_TAG_PREFIX = "create_group_chat"
internal const val CREATE_GROUP_CHAT_SCREEN_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$SELECTION_SCREEN_SUFFIX"
internal const val CREATE_GROUP_CHAT_LOADING_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$LOADING_SUFFIX"
internal const val CREATE_GROUP_CHAT_LIST_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$LIST_SUFFIX"
internal const val CREATE_GROUP_CHAT_EMPTY_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$EMPTY_SUFFIX"
internal const val CREATE_GROUP_CHAT_NEXT_FAB_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$NEXT_FAB_SUFFIX"
internal const val CREATE_GROUP_CHAT_SETTINGS_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$SETTINGS_SUFFIX"
internal const val CREATE_GROUP_CHAT_NAME_INPUT_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$NAME_INPUT_SUFFIX"
internal const val CREATE_GROUP_CHAT_EKR_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$EKR_SUFFIX"
internal const val CREATE_GROUP_CHAT_CHAT_LINK_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$CHAT_LINK_SUFFIX"
internal const val CREATE_GROUP_CHAT_ALLOW_ADD_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$ALLOW_ADD_SUFFIX"
internal const val CREATE_GROUP_CHAT_SELECTED_CONTACTS_TAG =
    "$CREATE_GROUP_CHAT_TAG_PREFIX$SELECTED_CONTACTS_SUFFIX"
internal const val CREATE_GROUP_CHAT_SETTINGS_LIST_TAG =
    "$CREATE_GROUP_CHAT_TAG_PREFIX$SETTINGS_LIST_SUFFIX"
internal const val CREATE_GROUP_CHAT_CONFIRM_FAB_TAG = "$CREATE_GROUP_CHAT_TAG_PREFIX$CONFIRM_FAB_SUFFIX"

private class CreateGroupChatUiStateProvider : PreviewParameterProvider<CreateChatUiState> {
    override val values: Sequence<CreateChatUiState> = sequenceOf(
        CreateChatUiState.Loading,
        CreateChatUiState.Data(
            contacts = persistentListOf(),
            query = null,
            allowGroupImageSelection = true,
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
            allowGroupImageSelection = true,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun CreateGroupChatScreenSelectionStepPreview(
    @PreviewParameter(CreateGroupChatUiStateProvider::class) state: CreateChatUiState,
) {
    AndroidThemeForPreviews {
        CreateGroupChatScreenContent(
            state = state,
            allowEmptyGroup = false,
            onSearchQueryChange = {},
            onConfirm = { _, _, _, _, _, _ -> },
            onBack = {},
            stepChange = {},
            step = CreateGroupChatStep.Selection,
            selectionState = rememberContactSelectionState()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateGroupChatScreenSettingsStepPreview(
) {
    val state = CreateChatUiState.Data(
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
        allowGroupImageSelection = true,
    )

    AndroidThemeForPreviews {
        CreateGroupChatScreenContent(
            state = state,
            allowEmptyGroup = false,
            onSearchQueryChange = {},
            onConfirm = { _, _, _, _, _, _ -> },
            onBack = {},
            stepChange = {},
            step = CreateGroupChatStep.Settings,
            selectionState = rememberContactSelectionState(initialSelectedHandles = setOf(1L))
        )
    }
}
