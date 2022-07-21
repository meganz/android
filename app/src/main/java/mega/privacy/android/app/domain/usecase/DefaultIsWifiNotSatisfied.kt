package mega.privacy.android.app.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.app.utils.wrapper.IsOnWifiWrapper
import javax.inject.Inject

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
class DefaultIsWifiNotSatisfied @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isOnWifiWrapper: IsOnWifiWrapper,
    @ApplicationContext private val context: Context,
) : IsWifiNotSatisfied {
    override fun invoke(): Boolean =
        cameraUploadRepository.isSyncByWifiDefault() && !isOnWifiWrapper.isOnWifi(context)
}
