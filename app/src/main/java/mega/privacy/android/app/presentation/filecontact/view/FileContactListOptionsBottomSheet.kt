package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContactListOptionsBottomSheet(
    recipient: ShareRecipient,
    onDismissSheet: () -> Unit,
    navigateToInfo: () -> Unit,
    updatePermissions: () -> Unit,
    removeShare: () -> Unit,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier,
    ) {
        ShareRecipientsOptionsContent(
            recipient = recipient,
            allowChangePermission = true,
            onInfoClicked = if (recipient is ShareRecipient.Contact) {
                {
                    navigateToInfo()
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissSheet()
                        }
                    }
                }
            } else null,
            onChangePermissionClicked = {
                updatePermissions()
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissSheet()
                    }
                }
            },
            onRemoveClicked = {
                removeShare()
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissSheet()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileContactListOptionsBottomSheetPreview() {
    AndroidThemeForPreviews {

        val contact = ShareRecipient.Contact(
            handle = 1L,
            email = "contact@email.com",
            contactData = ContactData(
                fullName = "Contact Name",
                alias = "Contact Alias",
                avatarUri = null,
                userVisibility = UserVisibility.Visible,
            ),
            isVerified = true,
            permission = AccessPermission.READ,
            isPending = false,
            status = UserChatStatus.Online,
            defaultAvatarColor = 0,
        )
        val sheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded
        )

        FileContactListOptionsBottomSheet(
            recipient = contact,
            onDismissSheet = {},
            navigateToInfo = {},
            updatePermissions = {},
            removeShare = {},
            coroutineScope = rememberCoroutineScope(),
            sheetState = sheetState,
        )
    }
}