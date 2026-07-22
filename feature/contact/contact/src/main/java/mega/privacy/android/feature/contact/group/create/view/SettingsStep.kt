package mega.privacy.android.feature.contact.group.create.view

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Group-settings step of the create-group-chat flow: group name, encryption key rotation (EKR),
 * get-chat-link and allow-add-participants toggles, plus the selected participants list.
 *
 * @param contacts the full contact list (used to resolve the selected participants).
 * @param selectedHandles the currently selected contact handles.
 * @param selectedCount the number of selected items.
 * @param tagPrefix the prefix for this step's test tags.
 * @param onConfirm invoked with the chosen settings.
 * @param onRemoveParticipant invoked with the handle of a participant removed from the list.
 * @param onBack invoked when the user navigates back to the selection step.
 * @param allowGroupImageSelection
 * @param modifier
 * @param initialChatLink initial state of the get-chat-link toggle (for previews/tests).
 * @param initialConfirmAttempted whether a confirm has already been attempted (for previews/tests).
 */
@Composable
internal fun SettingsStep(
    contacts: ImmutableSet<ContactItemUiState>,
    selectedHandles: ImmutableSet<Long>,
    selectedCount: Int,
    tagPrefix: String,
    onConfirm: (
        title: String?,
        isEkr: Boolean,
        isChatLink: Boolean,
        allowAddParticipants: Boolean,
        imageUri: String?,
    ) -> Unit,
    onRemoveParticipant: (Long) -> Unit,
    onBack: () -> Unit,
    allowGroupImageSelection: Boolean,
    modifier: Modifier = Modifier,
    initialChatLink: Boolean = false,
    initialConfirmAttempted: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var groupName by rememberSaveable { mutableStateOf("") }
    var isEkr by rememberSaveable { mutableStateOf(false) }
    var isChatLink by rememberSaveable { mutableStateOf(initialChatLink) }
    var allowAddParticipants by rememberSaveable { mutableStateOf(true) }
    var groupImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
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
                            groupImageUri?.toString(),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.x16),
                    horizontalArrangement = Arrangement.spacedBy(spacing.x16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (allowGroupImageSelection) {
                        GroupImagePicker(
                            imageUri = groupImageUri,
                            onImagePicked = { groupImageUri = it },
                        )
                    }
                    TextInputField(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("$tagPrefix$NAME_INPUT_SUFFIX"),
                        text = groupName,
                        placeholder = stringResource(sharedR.string.create_group_chat_name_hint),
                        keyboardType = KeyboardType.Text,
                        maxCharLimit = MAX_GROUP_NAME_LENGTH,
                        onValueChanged = { groupName = it },
                        imeAction = ImeAction.Done,
                        errorText = if (confirmAttempted && nameRequired) {
                            stringResource(sharedR.string.create_group_chat_link_requires_name_error)
                        } else {
                            null
                        },
                    )
                }
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
                    ContactItemView(
                        contactItemUiState = contact,
                        onRemoveClicked = { onRemoveParticipant(contact.handle) },
                    )
                }
            }
        }
    }
}

/**
 * Maximum length of a group chat name, matching the limit enforced across MEGA clients.
 */
private const val MAX_GROUP_NAME_LENGTH = 28

internal const val SETTINGS_SUFFIX = "_screen:settings"
internal const val NAME_INPUT_SUFFIX = "_screen:name_input"
internal const val EKR_SUFFIX = "_screen:ekr"
internal const val CHAT_LINK_SUFFIX = "_screen:chat_link"
internal const val ALLOW_ADD_SUFFIX = "_screen:allow_add"
internal const val SELECTED_CONTACTS_SUFFIX = "_screen:selected_contacts"
internal const val SETTINGS_LIST_SUFFIX = "_screen:settings_list"
internal const val CONFIRM_FAB_SUFFIX = "_screen:confirm_fab"

@CombinedThemePreviews
@Composable
private fun SettingsStepPreview() {
    val contacts = listOf(
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
    ).toImmutableSet()

    AndroidThemeForPreviews {
        SettingsStep(
            contacts = contacts,
            selectedHandles = setOf(1L, 2L).toImmutableSet(),
            selectedCount = 2,
            tagPrefix = "preview",
            onConfirm = { _, _, _, _, _ -> },
            onRemoveParticipant = {},
            onBack = {},
            allowGroupImageSelection = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SettingsStepPreviewNoAvatar() {
    val contacts = listOf(
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
    ).toImmutableSet()

    AndroidThemeForPreviews {
        SettingsStep(
            contacts = contacts,
            selectedHandles = setOf(1L, 2L).toImmutableSet(),
            selectedCount = 2,
            tagPrefix = "preview",
            onConfirm = { _, _, _, _, _ -> },
            onRemoveParticipant = {},
            onBack = {},
            allowGroupImageSelection = false,
        )
    }
}
