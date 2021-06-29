package mega.privacy.android.app.textFileEditor

import mega.privacy.android.app.R
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.EDIT_MODE
import mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

object TextEditorUtil {

    @JvmStatic
    fun getCreationOrEditorText(transfer: MegaTransfer, error: MegaError): String {
        val appData = transfer.appData
        val appDataParts = appData.split(APP_DATA_INDICATOR).toTypedArray()
        val successful = error.errorCode == MegaError.API_OK

        return when {
            appDataParts[1] == EDIT_MODE -> getString(if (successful) R.string.file_updated else R.string.file_update_failed)
            appDataParts[2].toBoolean() -> getString(
                if (successful) R.string.file_saved_to else R.string.file_saved_to_failed,
                getString(R.string.section_cloud_drive)
            )
            else -> getString(if (successful) R.string.file_created else R.string.file_creation_failed)
        }
    }
}