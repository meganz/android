package mega.privacy.android.app.presentation.fileinfo.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Some actions initiated by the user needs some extra steps, like a confirmation dialog, or choose the desired access permission
 */
internal sealed class FileInfoExtraAction {

    sealed class ConfirmRemove : FileInfoExtraAction() {
        abstract val text: String
            @Composable get

        sealed class RemoveNode(@StringRes val textRes: Int) : ConfirmRemove() {
            override val text: String
                @Composable get() = stringResource(id = textRes)
        }

        object SendToRubbish : RemoveNode(
            textRes = R.string.confirmation_move_to_rubbish,
        )

        object SendToRubbishCameraUploads : RemoveNode(
            textRes = R.string.confirmation_move_cu_folder_to_rubbish,
        )

        object SendToRubbishSecondaryMediaUploads : RemoveNode(
            textRes = R.string.confirmation_move_mu_folder_to_rubbish,
        )

        object Delete : RemoveNode(
            textRes = R.string.confirmation_delete_from_mega,
        )

        object DeleteLink : ConfirmRemove() {
            override val text: String
                @Composable get() = stringResource(id = R.string.context_remove_link_warning_text)
        }

        data class DeleteContact(val emails: List<String>) : ConfirmRemove() {
            override val text: String
                @Composable get() {
                    return emails.singleOrNull()?.let { singleContact ->
                        stringResource(R.string.remove_contact_shared_folder, singleContact)
                    } ?: pluralStringResource(
                        R.plurals.remove_multiple_contacts_shared_folder,
                        emails.size,
                        emails.size
                    )
                }
        }
    }

    data class ChangePermission(val emails: List<String>, val selected: AccessPermission?) :
        FileInfoExtraAction()
}