package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper.Companion.APP_DATA_INDICATOR
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper.Companion.APP_DATA_SEPARATOR
import mega.privacy.android.domain.entity.transfer.TransferAppData
import javax.inject.Inject

/**
 * [TransferAppData] to [String] mapper. It maps the [TransferAppData] to the format expected by sdk.
 */
internal
class TransferAppDataStringMapper @Inject constructor() {
    /**
     * Get a [String] corresponding to a List of [TransferAppData] to use as raw appData in sdk
     * @param transferAppDataList a [List] of [TransferAppData]
     * @return the app data [String] in the format expected by sdk
     */
    operator fun invoke(
        transferAppDataList: List<TransferAppData>?,
    ): String? = if (transferAppDataList.isNullOrEmpty()) {
        null
    } else {
        transferAppDataList.joinToString(APP_DATA_SEPARATOR) {
            it.keyAndValues().joinToString(APP_DATA_INDICATOR)
        }
    }

    private fun TransferAppData.keyAndValues(): List<String> =
        listOf(this.rawString()).plus(
            when (this) {
                is TransferAppData.ChatUpload -> listOf(pendingMessageId)
                is TransferAppData.SdCardDownload -> listOf(targetPath, targetUri)
                is TransferAppData.TextFileUpload -> listOf(
                    TextFileModeConstants.getSdkValueFromFileDataMode(mode),
                    fromHomePage.toString()
                )

                else -> emptyList()
            }
        ).filterNotNull()

    private fun TransferAppData.rawString() =
        AppDataTypeConstants.getTypeFromTransferAppDataClass(this::class)?.sdkTypeValue
}