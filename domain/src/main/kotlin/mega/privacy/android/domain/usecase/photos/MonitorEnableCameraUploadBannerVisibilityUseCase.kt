package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

typealias IsBannerVisible = Boolean

class MonitorEnableCameraUploadBannerVisibilityUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val environmentRepository: EnvironmentRepository,
    photosRepository: PhotosRepository,
) {

    val enableCameraUploadBannerVisibilityFlow: Flow<IsBannerVisible> =
        photosRepository.enableCameraUploadBannerDismissedTimestamp
            .map { dismissedTimestamp ->
                if (dismissedTimestamp == null) {
                    true
                } else {
                    val now = environmentRepository.now
                    val timeDelta = now - dismissedTimestamp
                    timeDelta >= FIFTEEN_DAYS_DISMISSAL_THRESHOLD
                }
            }
            .flowOn(ioDispatcher)

    companion object {
        private const val FIFTEEN_DAYS_DISMISSAL_THRESHOLD = 15 * 24 * 60 * 60 * 1000
    }
}
