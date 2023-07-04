package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.BackgroundTransfer
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.CameraUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.ChatUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.SDCardDownload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.TextFileUpload
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants.VoiceClip
import mega.privacy.android.domain.entity.transfer.TransferAppData
import timber.log.Timber
import javax.inject.Inject

/**
 * Raw app data [String] to a list of [TransferAppData] mapper
 */
internal class TransferAppDataMapper @Inject constructor() {
    /**
     * Get a list of [TransferAppData] corresponding to raw appData [String]
     * @param appDataRaw the app data [String] as it is in MegaTransfer
     * @return a [List] of [TransferAppData]
     */
    operator fun invoke(
        appDataRaw: String,
    ): List<TransferAppData> = if (appDataRaw.isEmpty()) emptyList() else {
        appDataRaw
            .split(APP_DATA_REPEATED_TRANSFER_SEPARATOR)
            .flatMap { it.split(APP_DATA_SEPARATOR) }
            .map { appDataRawSingle ->
                val typeAndValues = appDataRawSingle.split(APP_DATA_INDICATOR)
                AppDataTypeConstants.getTypeFromSdkValue(typeAndValues.first()) to
                        typeAndValues.drop(1)
            }
            .mapNotNull { (type, values) ->
                val result = when (type) {
                    VoiceClip -> TransferAppData.VoiceClip
                    CameraUpload -> TransferAppData.CameraUpload
                    ChatUpload -> values.firstIfNotBlank()
                        ?.let { TransferAppData.ChatUpload(it) }

                    SDCardDownload -> {
                        values.firstIfNotBlank()?.let {
                            TransferAppData.SdCardDownload(it, values.getOrNull(1))
                        }
                    }

                    TextFileUpload -> {
                        TextFileModeConstants.getFileDataModeFromSdkValue(values.firstIfNotBlank())
                            ?.let { mode ->
                                values.getOrNull(1)?.toBooleanStrictOrNull()?.let { home ->
                                    TransferAppData.TextFileUpload(mode, home)
                                }
                            }
                    }

                    BackgroundTransfer -> TransferAppData.BackgroundTransfer

                    else -> null
                }
                if (result == null) {
                    Timber.d("appData not recognized: $type $values")
                }
                return@mapNotNull result
            }
    }

    private fun List<String>.firstIfNotBlank() = this.firstOrNull()?.takeIf { it.isNotBlank() }

    companion object {

        /**
         * App data for indicating the data after it, is the value of a transfer parameter
         */
        const val APP_DATA_INDICATOR = ">"

        /**
         * App data for indicating the data after it, is a new transfer parameter.
         */
        const val APP_DATA_SEPARATOR = "-"

        /**
         * App data for indicating the data after it, is a new AppData due to a repeated transfer.
         */
        const val APP_DATA_REPEATED_TRANSFER_SEPARATOR = "!"
    }
}