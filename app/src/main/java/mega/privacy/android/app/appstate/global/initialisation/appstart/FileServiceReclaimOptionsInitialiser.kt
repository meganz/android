package mega.privacy.android.app.appstate.global.initialisation.appstart

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import mega.privacy.android.domain.usecase.fileservice.SetFileServiceReclaimOptionsUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * App start initialiser that configures file service reclaim options.
 * @property setFileServiceReclaimOptionsUseCase
 */
class FileServiceReclaimOptionsInitialiser @Inject constructor(
    private val setFileServiceReclaimOptionsUseCase: SetFileServiceReclaimOptionsUseCase,
) : AppStartInitialiserAction(action = {
    runCatching {
        setFileServiceReclaimOptionsUseCase(
            options = FileServiceReclaimOptions(
                reclaimAgeThreshold = 5.minutes,
                reclaimDelay = 1.minutes,
                reclaimPeriod = 5.minutes,
                reclaimSizeThreshold = 100L * 1024 * 1024,
                reclaimTarget = 0L,
            ),
        )
    }.onFailure {
        Timber.e(it, "Error setting file service reclaim options")
    }
})
