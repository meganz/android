package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.fcm.FcmManager
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser
import javax.inject.Inject

/**
 * Subscribes the app to the all-users FCM topic at app create.
 */
class FcmTopicInitialiser @Inject constructor(
    private val fcmManager: FcmManager,
) : AppCreateInitialiser {
    override val name = "FcmTopicInitialiser"
    override val isCritical = false

    override suspend operator fun invoke() {
        fcmManager.subscribeToAllUsersTopic()
    }
}
