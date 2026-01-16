package mega.privacy.android.data.mapper.transfer.upload

import mega.privacy.android.data.mapper.pitag.PitagTargetMapper
import mega.privacy.android.data.mapper.pitag.PitagTriggerMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.TransferAppData
import nz.mega.sdk.MegaUploadOptions.INVALID_CUSTOM_MOD_TIME
import javax.inject.Inject

internal class MegaUploadOptionsMapper @Inject constructor(
    private val transferAppDataStringMapper: TransferAppDataStringMapper,
    private val pitagTriggerMapper: PitagTriggerMapper,
    private val pitagTargetMapper: PitagTargetMapper,
    private val megaUploadOptionsProvider: MegaUploadOptionsProvider,
) {

    operator fun invoke(
        fileName: String?,
        mtime: Long?,
        appData: List<TransferAppData>?,
        isSourceTemporary: Boolean,
        startFirst: Boolean,
        pitagTrigger: PitagTrigger,
        pitagTarget: PitagTarget,
    ) = megaUploadOptionsProvider()?.apply {
        fileName?.let { this.fileName = fileName }
        this.mtime = mtime ?: INVALID_CUSTOM_MOD_TIME
        appData?.let { this.appData = transferAppDataStringMapper(appData) }
        this.isSourceTemporary = isSourceTemporary
        this.startFirst = startFirst
        this.pitagTrigger = pitagTriggerMapper(pitagTrigger)
        this.pitagTarget = pitagTargetMapper(pitagTarget)
    }
}