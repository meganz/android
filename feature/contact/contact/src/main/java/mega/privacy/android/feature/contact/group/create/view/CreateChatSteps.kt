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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Selection step of a create-chat flow: search and toggle MEGA contacts. Reused by the "create group
 * chat" and "new chat" screens; the [title], FAB [fabIcon] and [tagPrefix] differ per flow.
 *
 * @param state the searchable contacts state.
 * @param selectedHandles the currently selected contact handles.
 * @param selectedCount the number of selected items.
 * @param emptySelectionTitle the top-bar title shown when nothing is selected.
 * @param fabIcon the FAB icon painter.
 * @param tagPrefix the prefix for this step's test tags.
 * @param showFab whether the FAB should be shown for the current selection.
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onToggle invoked with the handle of the contact whose selection is toggled.
 * @param onNext invoked when the FAB is tapped.
 * @param onBack invoked when the user navigates back.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionStep(
    state: CreateChatUiState,
    selectedHandles: Set<Long>,
    selectedCount: Int,
    emptySelectionTitle: String,
    fabIcon: Painter,
    tagPrefix: String,
    showFab: Boolean,
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
            .testTag("$tagPrefix$SELECTION_SCREEN_SUFFIX"),
        topBar = {
            val title = if (selectedCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_selected,
                    selectedCount,
                    selectedCount,
                )
            } else {
                emptySelectionTitle
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
            if (state is CreateChatUiState.Data && showFab) {
                MegaFab(
                    modifier = Modifier
                        .testTag("$tagPrefix$NEXT_FAB_SUFFIX")
                        .applyScrollToHideFabBehavior(),
                    onClick = {
                        onSearchQueryChange(null)
                        onNext()
                    },
                    painter = fabIcon,
                )
            }
        },
    ) { padding ->
        when (state) {
            CreateChatUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("$tagPrefix$LOADING_SUFFIX"),
            )

            is CreateChatUiState.Data -> {
                if (state.isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag("$tagPrefix$EMPTY_SUFFIX"),
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
                            .testTag("$tagPrefix$LIST_SUFFIX"),
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

/**
 * Group-settings step of the create-group-chat flow: group name, encryption key rotation (EKR),
 * get-chat-link and allow-add-participants toggles, plus the selected participants list.
 *
 * @param contacts the full contact list (used to resolve the selected participants).
 * @param selectedHandles the currently selected contact handles.
 * @param selectedCount the number of selected items.
 * @param tagPrefix the prefix for this step's test tags.
 * @param onConfirm invoked with the chosen settings.
 * @param onBack invoked when the user navigates back to the selection step.
 * @param modifier
 * @param initialChatLink initial state of the get-chat-link toggle (for previews/tests).
 * @param initialConfirmAttempted whether a confirm has already been attempted (for previews/tests).
 */
@Composable
internal fun SettingsStep(
    contacts: List<ContactItemUiState>,
    selectedHandles: Set<Long>,
    selectedCount: Int,
    tagPrefix: String,
    onConfirm: (
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialChatLink: Boolean = false,
    initialConfirmAttempted: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var groupName by rememberSaveable { mutableStateOf("") }
    var isEkr by rememberSaveable { mutableStateOf(false) }
    var isChatLink by rememberSaveable { mutableStateOf(initialChatLink) }
    var allowAddParticipants by rememberSaveable { mutableStateOf(true) }
    var confirmAttempted by rememberSaveable { mutableStateOf(initialConfirmAttempted) }
    val selectedContacts = remember(contacts, selectedHandles) {
        contacts.filter { it.handle in selectedHandles }
    }
    val chatLinkEnabled = !isEkr && isChatLink
    val nameRequired = chatLinkEnabled && groupName.isBlank()

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("$tagPrefix$SETTINGS_SUFFIX"),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.create_group_chat_settings_title),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
        floatingActionButton = {
            MegaFab(
                modifier = Modifier.testTag("$tagPrefix$CONFIRM_FAB_SUFFIX"),
                onClick = {
                    confirmAttempted = true
                    if (!nameRequired) {
                        onConfirm(
                            groupName.trim().ifBlank { null },
                            isEkr,
                            chatLinkEnabled,
                            allowAddParticipants,
                        )
                    }
                },
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("$tagPrefix$SETTINGS_LIST_SUFFIX"),
            verticalArrangement = Arrangement.spacedBy(spacing.x8),
        ) {
            item {
                TextInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.x16)
                        .testTag("$tagPrefix$NAME_INPUT_SUFFIX"),
                    label = stringResource(sharedR.string.create_group_chat_name_hint),
                    text = groupName,
                    keyboardType = KeyboardType.Text,
                    onValueChanged = { groupName = it },
                    imeAction = ImeAction.Done,
                    errorText = if (confirmAttempted && nameRequired) {
                        stringResource(sharedR.string.create_group_chat_link_requires_name_error)
                    } else {
                        null
                    },
                )
            }

            item {
                GenericListItem(
                    modifier = Modifier.testTag("$tagPrefix$EKR_SUFFIX"),
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
                        modifier = Modifier.testTag("$tagPrefix$CHAT_LINK_SUFFIX"),
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
                    modifier = Modifier.testTag("$tagPrefix$ALLOW_ADD_SUFFIX"),
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
                            .testTag("$tagPrefix$SELECTED_CONTACTS_SUFFIX"),
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

internal const val SELECTION_SCREEN_SUFFIX = "_screen"
internal const val LOADING_SUFFIX = "_screen:loading"
internal const val LIST_SUFFIX = "_screen:list"
internal const val EMPTY_SUFFIX = "_screen:empty"
internal const val NEXT_FAB_SUFFIX = "_screen:next_fab"
internal const val SETTINGS_SUFFIX = "_screen:settings"
internal const val NAME_INPUT_SUFFIX = "_screen:name_input"
internal const val EKR_SUFFIX = "_screen:ekr"
internal const val CHAT_LINK_SUFFIX = "_screen:chat_link"
internal const val ALLOW_ADD_SUFFIX = "_screen:allow_add"
internal const val SELECTED_CONTACTS_SUFFIX = "_screen:selected_contacts"
internal const val SETTINGS_LIST_SUFFIX = "_screen:settings_list"
internal const val CONFIRM_FAB_SUFFIX = "_screen:confirm_fab"
