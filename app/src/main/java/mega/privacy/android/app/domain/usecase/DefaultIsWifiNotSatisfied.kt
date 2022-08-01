package mega.privacy.android.app.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.wrapper.IsOnWifiWrapper
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Check if camera upload is by wifi only, but not using wifi
 *
 * @return true, if wifi constraint is not satisfied
 */
class DefaultIsWifiNotSatisfied @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isOnWifiWrapper: IsOnWifiWrapper,
    @ApplicationContext private val context: Context,
) : IsWifiNotSatisfied {
    override fun invoke(): Boolean =
        cameraUploadRepository.isSyncByWifiDefault() && !isOnWifiWrapper.isOnWifi(context)
}
