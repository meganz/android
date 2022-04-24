package mega.privacy.android.app.jobservices

import javax.inject.Inject

/**
 * The implementation of [CameraUploadsServiceWrapper]
 */
class CameraUploadsServiceFacade @Inject constructor() : CameraUploadsServiceWrapper {

    override fun isServiceRunning(): Boolean = CameraUploadsService.isServiceRunning()
}
