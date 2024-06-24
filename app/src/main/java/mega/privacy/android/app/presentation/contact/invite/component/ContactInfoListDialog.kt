package mega.privacy.android.app.presentation.contact.invite.component

import android.content.res.Configuration
import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun ContactInfoListDialog(
    contactInfo: InvitationContactInfo,
    currentSelectedContactInfo: List<InvitationContactInfo>,
    onConfirm: (selectedContactInfo: List<InvitationContactInfo>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = onCancel,
) {
    val selectedContactInfo = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf<InvitationContactInfo>().apply {
            addAll(currentSelectedContactInfo)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxHeight(
                    when (LocalConfiguration.current.orientation) {
                        Configuration.ORIENTATION_PORTRAIT -> 0.5F
                        else -> 0.9F
                    }
                )
                .testTag(CONTACT_INFO_LIST_DIALOG_TAG),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MegaText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp)
                        .testTag(CONTACT_NAME_TAG),
                    text = contactInfo.getContactName(),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.h6
                )

                ContactsList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F)
                        .testTag(CONTACT_LIST_TAG),
                    contactInfo = contactInfo,
                    selectedContacts = selectedContactInfo,
                    onCheckedChange = { contact, isChecked ->
                        if (isChecked) {
                            selectedContactInfo.add(contact)
                        } else {
                            selectedContactInfo.remove(contact)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextMegaButton(
                        modifier = Modifier.testTag(CANCEL_BUTTON_TAG),
                        text = stringResource(id = R.string.general_cancel),
                        onClick = onCancel,
                    )
                    TextMegaButton(
                        modifier = Modifier.testTag(OK_BUTTON_TAG),
                        text = stringResource(id = R.string.general_ok),
                        onClick = { onConfirm(selectedContactInfo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contactInfo: InvitationContactInfo,
    selectedContacts: List<InvitationContactInfo>,
    onCheckedChange: (contactInfo: InvitationContactInfo, isChecked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = contactInfo.filteredContactInfos,
            key = { index, _ -> index }
        ) { _, contact ->
            val isChecked = selectedContacts.any {
                it.displayInfo == contact && contactInfo.id == it.id
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            onCheckedChange(
                                contactInfo.copy(displayInfo = contact),
                                !isChecked
                            )
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MegaCheckbox(
                    modifier = Modifier
                        .padding(start = 30.dp)
                        .alpha(if (isChecked) 1F else 0.3F)
                        .testTag(CHECKBOX_TAG + contact),
                    checked = isChecked,
                    onCheckedChange = {
                        onCheckedChange(
                            contactInfo.copy(displayInfo = contact),
                            it
                        )
                    },
                    rounded = false
                )

                val formattedContact = if (isEmail(contact)) {
                    contact
                } else {
                    // Stripping separators only for phone numbers.
                    PhoneNumberUtils.stripSeparators(contact)
                }
                MegaText(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .testTag(CONTACT_ITEM_TAG + formattedContact),
                    text = formattedContact,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }
    }
}

private fun isEmail(contact: String) =
    contact.isNotBlank() && Constants.EMAIL_ADDRESS.matcher(contact).matches()

@CombinedTextAndThemePreviews
@Composable
private fun ContactInfoListDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ContactInfoListDialog(
            contactInfo = InvitationContactInfo(
                name = "W3 Multiple Phones",
                filteredContactInfos = listOf(
                    "1231231231",
                    "email@email.email"
                )
            ),
            currentSelectedContactInfo = emptyList(),
            onConfirm = {},
            onCancel = {}
        )
    }
}

internal const val CONTACT_INFO_LIST_DIALOG_TAG = "contact_info_list_dialog:dialog"
internal const val CONTACT_NAME_TAG = "contact_info_list_dialog:text_contact_name"
internal const val CONTACT_LIST_TAG = "contact_info_list_dialog:list_contact"
internal const val CANCEL_BUTTON_TAG = "contact_info_list_dialog:button_cancel"
internal const val OK_BUTTON_TAG = "contact_info_list_dialog:button_ok"
internal const val CHECKBOX_TAG = "contact_list:checkbox_select_a_contact"
internal const val CONTACT_ITEM_TAG = "contact_list:text_contact_item"
