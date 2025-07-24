package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeMapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError
import javax.inject.Inject

internal class CompletedTransferModelMapper @Inject constructor(
    private val stringWrapper: StringWrapper,
    private val transferTypeMapper: TransferTypeMapper,
    private val transferStateMapper: TransferStateMapper,
    private val transferAppDataMapper: TransferAppDataMapper,
) {
    operator fun invoke(entity: CompletedTransferEntity): CompletedTransfer {
        val appData = entity.appData?.let { transferAppDataMapper(it) }
        return CompletedTransfer(
            id = entity.id,
            fileName = entity.fileName,
            type = transferTypeMapper(entity.type, appData ?: emptyList()),
            state = transferStateMapper(entity.state),
            size = entity.size,
            handle = entity.handle,
            path = entity.path,
            displayPath = entity.displayPath,
            isOffline = entity.isOffline,
            timestamp = entity.timestamp,
            error = entity.errorCode?.let {
                getErrorString(MegaException(entity.errorCode, entity.error))
            } ?: entity.error,
            errorCode = entity.errorCode,
            originalPath = entity.originalPath,
            parentHandle = entity.parentHandle,
            appData = appData,
        )
    }

    /**
     * Gets the localized error string to show as cause of the failure.
     */
    private fun getErrorString(error: MegaException): String =
        if (error.errorCode == API_EOVERQUOTA_FOREIGN)
            stringWrapper.getErrorStorageQuota()
        else
            stringWrapper.getErrorStringResource(error)
}

internal const val API_EOVERQUOTA_FOREIGN = -MegaError.API_EOVERQUOTA
