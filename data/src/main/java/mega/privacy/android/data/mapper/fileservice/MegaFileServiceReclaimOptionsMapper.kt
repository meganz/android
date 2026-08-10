package mega.privacy.android.data.mapper.fileservice

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import nz.mega.sdk.MegaFileServiceReclaimOptions
import javax.inject.Inject

/**
 * Mapper to convert [FileServiceReclaimOptions] to [MegaFileServiceReclaimOptions].
 */
internal class MegaFileServiceReclaimOptionsMapper @Inject constructor() {

    operator fun invoke(options: FileServiceReclaimOptions): MegaFileServiceReclaimOptions? =
        MegaFileServiceReclaimOptions.create()?.apply {
            ageThreshold = ((options.reclaimAgeThreshold.inWholeSeconds + 59) / 60).toInt()
            delay = options.reclaimDelay.inWholeSeconds
            period = options.reclaimPeriod.inWholeSeconds
            reclaimThreshold = options.reclaimSizeThreshold
            reclaimTarget = options.reclaimTarget
        }
}
