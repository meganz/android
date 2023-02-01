package mega.privacy.android.app.exportRK

import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * View model for ExportRecoveryKeyActivity
 * @see [ExportRecoveryKeyActivity]
 */
@HiltViewModel
class ExportRecoveryKeyViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : BaseRxViewModel() {

    /**
     * Exports the Recovery Key
     */
    fun exportRK(): String? {
        val textRK = megaApi.exportMasterKey()

        if (!isTextEmpty(textRK)) {
            megaApi.masterKeyExported(null)
        }

        return textRK
    }
}