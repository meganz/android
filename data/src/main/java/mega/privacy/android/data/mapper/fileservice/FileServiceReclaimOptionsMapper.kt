package mega.privacy.android.data.mapper.fileservice

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import nz.mega.sdk.MegaFileServiceReclaimOptions
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Mapper to convert [MegaFileServiceReclaimOptions] to [FileServiceReclaimOptions].
 */
internal class FileServiceReclaimOptionsMapper @Inject constructor() {
    operator fun invoke(sdkOptions: MegaFileServiceReclaimOptions): FileServiceReclaimOptions =
        FileServiceReclaimOptions(
            reclaimAgeThreshold = sdkOptions.ageThreshold.minutes,
            reclaimDelay = sdkOptions.delay.seconds,
            reclaimPeriod = sdkOptions.period.seconds,
            reclaimSizeThreshold = sdkOptions.reclaimThreshold,
            reclaimTarget = sdkOptions.reclaimTarget,
        )
}
