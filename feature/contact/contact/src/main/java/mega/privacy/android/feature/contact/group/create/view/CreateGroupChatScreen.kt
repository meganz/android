package mega.privacy.android.feature.contact.group.create.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.add.view.ContactSelectionState
import mega.privacy.android.feature.contact.add.view.rememberContactSelectionState
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.group.create.model.CreateGroupChatUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
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
    state: CreateGroupChatUiState,
    allowEmptyGroup: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
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
    state: CreateGroupChatUiState,
    allowEmptyGroup: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (
        selectedHandles: Set<Long>,
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
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
            allowEmptyGroup = allowEmptyGroup,
            onSearchQueryChange = onSearchQueryChange,
            onToggle = selectionState::toggleSelection,
            onNext = { stepChange(CreateGroupChatStep.Settings) },
            onBack = onBack,
            modifier = modifier,
        )

        CreateGroupChatStep.Settings -> SettingsStep(
            contacts = (state as? CreateGroupChatUiState.Data)?.contacts ?: emptyList(),
            selectedHandles = selectionState.selectedHandles,
            selectedCount = selectionState.selectedItemsCount,
            onConfirm = { title, isEkr, isChatLink, allowAddParticipants ->
                onConfirm(
                    selectionState.selectedHandles,
                    title,
                    isEkr,
                    isChatLink,
                    allowAddParticipants,
                )
            },
            onBack = { stepChange(CreateGroupChatStep.Selection) },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionStep(
    state: CreateGroupChatUiState,
    selectedHandles: Set<Long>,
    selectedCount: Int,
    allowEmptyGroup: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onToggle: (Long) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CREATE_GROUP_CHAT_SCREEN_TAG),
        topBar = {
            val title = if (selectedCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_selected,
                    selectedCount,
                    selectedCount,
                )
            } else {
                stringResource(sharedR.string.general_new_group_chat)
            }
            MegaSearchTopAppBar(
                title = title,
                navigationType = AppBarNavigationType.Back(onBack),
                query = searchText,
                isSearchingMode = searchActive,
                onQueryChanged = {
                    searchText = it
                    onSearchQueryChange(it.ifBlank { null })
                },
                onSearchingModeChanged = {
                    searchActive = it
                    if (!it && searchText.isNotEmpty()) {
                        searchText = ""
                        onSearchQueryChange(null)
                    }
                },
                searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
            )
        },
        floatingActionButton = {
            if (state is CreateGroupChatUiState.Data && (selectedCount > 0 || allowEmptyGroup)) {
                MegaFab(
                    modifier = Modifier
                        .testTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG)
                        .applyScrollToHideFabBehavior(),
                    onClick = onNext,
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                )
            }
        },
    ) { padding ->
        when (state) {
            CreateGroupChatUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(CREATE_GROUP_CHAT_LOADING_TAG),
            )

            is CreateGroupChatUiState.Data -> {
                if (state.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag(CREATE_GROUP_CHAT_EMPTY_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        MegaText(
                            text = stringResource(sharedR.string.contacts_empty_title),
                            textColor = TextColor.Secondary,
                        )
                    }
                } else {
                    val layoutDirection = LocalLayoutDirection.current
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(CREATE_GROUP_CHAT_LIST_TAG),
                        contentPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding(),
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = padding.calculateBottomPadding(),
                        ),
                    ) {
                        items(state.contacts, key = { it.handle }) { contact ->
                            ContactItemView(
                                contactItemUiState = contact,
                                onClick = { onToggle(contact.handle) },
                                selected = contact.handle in selectedHandles,
                                inSelectionMode = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsStep(
    contacts: List<ContactItemUiState>,
    selectedHandles: Set<Long>,
    selectedCount: Int,
    onConfirm: (
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var groupName by rememberSaveable { mutableStateOf("") }
    var isEkr by rememberSaveable { mutableStateOf(false) }
    var isChatLink by rememberSaveable { mutableStateOf(false) }
    var allowAddParticipants by rememberSaveable { mutableStateOf(true) }
    val selectedContacts = remember(contacts, selectedHandles) {
        contacts.filter { it.handle in selectedHandles }
    }

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(CREATE_GROUP_CHAT_SETTINGS_TAG),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.create_group_chat_settings_title),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
        floatingActionButton = {
            MegaFab(
                modifier = Modifier.testTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG),
                onClick = {
                    onConfirm(
                        groupName.trim().ifBlank { null },
                        isEkr,
                        if (isEkr) false else isChatLink,
                        allowAddParticipants,
                    )
                },
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(CREATE_GROUP_CHAT_SETTINGS_LIST_TAG),
            verticalArrangement = Arrangement.spacedBy(spacing.x8),
        ) {
            item {
                TextInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.x16)
                        .testTag(CREATE_GROUP_CHAT_NAME_INPUT_TAG),
                    label = stringResource(sharedR.string.create_group_chat_name_hint),
                    text = groupName,
                    keyboardType = KeyboardType.Text,
                    onValueChanged = { groupName = it },
                    imeAction = ImeAction.Done,
                )
            }

            item {
                GenericListItem(
                    modifier = Modifier.testTag(CREATE_GROUP_CHAT_EKR_TAG),
                    title = {
                        MegaText(
                            text = stringResource(sharedR.string.create_group_chat_ekr_label),
                            textColor = TextColor.Primary,
                            style = AppTheme.typography.bodyLarge,
                        )
                    },
                    subtitle = {
                        MegaText(
                            text = stringResource(sharedR.string.create_group_chat_ekr_explanation),
                            textColor = TextColor.Secondary,
                            style = AppTheme.typography.bodyMedium,
                        )
                    },
                    onClickListener = { isEkr = !isEkr },
                    trailingElement = {
                        Toggle(isChecked = isEkr, onCheckedChange = { isEkr = it })
                    },
                )
            }

            if (!isEkr) {
                item {
                    FlexibleLineListItem(
                        modifier = Modifier.testTag(CREATE_GROUP_CHAT_CHAT_LINK_TAG),
                        title = stringResource(sharedR.string.create_group_chat_get_chat_link_label),
                        onClickListener = { isChatLink = !isChatLink },
                        trailingElement = {
                            Toggle(
                                isChecked = isChatLink,
                                onCheckedChange = { isChatLink = it },
                            )
                        },
                    )
                }
            }

            item {
                FlexibleLineListItem(
                    modifier = Modifier.testTag(CREATE_GROUP_CHAT_ALLOW_ADD_TAG),
                    title = stringResource(sharedR.string.create_group_chat_allow_add_participants_label),
                    onClickListener = { allowAddParticipants = !allowAddParticipants },
                    trailingElement = {
                        Toggle(
                            isChecked = allowAddParticipants,
                            onCheckedChange = { allowAddParticipants = it },
                        )
                    },
                )
            }

            if (selectedContacts.isNotEmpty()) {
                item {
                    MegaText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.x16)
                            .testTag(CREATE_GROUP_CHAT_SELECTED_CONTACTS_TAG),
                        text = pluralStringResource(
                            sharedR.plurals.general_number_participants,
                            selectedCount,
                            selectedCount,
                        ),
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.labelMedium,
                    )
                }

                items(selectedContacts, key = { it.handle }) { contact ->
                    ContactItemView(contactItemUiState = contact)
                }
            }
        }
    }
}

internal enum class CreateGroupChatStep { Selection, Settings }

internal const val CREATE_GROUP_CHAT_SCREEN_TAG = "create_group_chat_screen"
internal const val CREATE_GROUP_CHAT_LOADING_TAG = "create_group_chat_screen:loading"
internal const val CREATE_GROUP_CHAT_LIST_TAG = "create_group_chat_screen:list"
internal const val CREATE_GROUP_CHAT_EMPTY_TAG = "create_group_chat_screen:empty"
internal const val CREATE_GROUP_CHAT_NEXT_FAB_TAG = "create_group_chat_screen:next_fab"
internal const val CREATE_GROUP_CHAT_SETTINGS_TAG = "create_group_chat_screen:settings"
internal const val CREATE_GROUP_CHAT_NAME_INPUT_TAG = "create_group_chat_screen:name_input"
internal const val CREATE_GROUP_CHAT_EKR_TAG = "create_group_chat_screen:ekr"
internal const val CREATE_GROUP_CHAT_CHAT_LINK_TAG = "create_group_chat_screen:chat_link"
internal const val CREATE_GROUP_CHAT_ALLOW_ADD_TAG = "create_group_chat_screen:allow_add"
internal const val CREATE_GROUP_CHAT_SELECTED_CONTACTS_TAG = "create_group_chat_screen:selected_contacts"
internal const val CREATE_GROUP_CHAT_SETTINGS_LIST_TAG = "create_group_chat_screen:settings_list"
internal const val CREATE_GROUP_CHAT_CONFIRM_FAB_TAG = "create_group_chat_screen:confirm_fab"

private class CreateGroupChatUiStateProvider : PreviewParameterProvider<CreateGroupChatUiState> {
    override val values: Sequence<CreateGroupChatUiState> = sequenceOf(
        CreateGroupChatUiState.Loading,
        CreateGroupChatUiState.Data(contacts = persistentListOf(), query = null),
        CreateGroupChatUiState.Data(
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
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun CreateGroupChatScreenSelectionStepPreview(
    @PreviewParameter(CreateGroupChatUiStateProvider::class) state: CreateGroupChatUiState,
) {
    AndroidThemeForPreviews {
        CreateGroupChatScreenContent(
            state = state,
            allowEmptyGroup = false,
            onSearchQueryChange = {},
            onConfirm = { _, _, _, _, _ -> },
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
    val state = CreateGroupChatUiState.Data(
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
    )

    AndroidThemeForPreviews {
        CreateGroupChatScreenContent(
            state = state,
            allowEmptyGroup = false,
            onSearchQueryChange = {},
            onConfirm = { _, _, _, _, _ -> },
            onBack = {},
            stepChange = {},
            step = CreateGroupChatStep.Settings,
            selectionState = rememberContactSelectionState(initialSelectedHandles = setOf(1L))
        )
    }
}
