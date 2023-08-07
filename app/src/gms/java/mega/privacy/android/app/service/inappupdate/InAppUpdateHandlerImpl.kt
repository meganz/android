package mega.privacy.android.app.service.inappupdate

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.app.middlelayer.inappupdate.InAppUpdateHandler
import timber.log.Timber
import javax.inject.Inject

/**
 * [InAppUpdateHandler] Implementation
 */
class InAppUpdateHandlerImpl @Inject constructor(@ActivityContext context: Context) :
    InAppUpdateHandler {
    private val appUpdateManager: AppUpdateManager by lazy(LazyThreadSafetyMode.NONE) {
        AppUpdateManagerFactory.create(
            context
        )
    }
    private val updateFlowResultLauncher: ActivityResultLauncher<IntentSenderRequest>? =
        (context as? ComponentActivity)?.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                AppCompatActivity.RESULT_OK -> Timber.d("InAppUpdate: The user has accepted the update")
                AppCompatActivity.RESULT_CANCELED -> Timber.d("InAppUpdate: The user has denied or canceled the update.")
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> Timber.d("InAppUpdate: Unknown error")
            }
        }
    private val appUpdateRequestCode = 123

    /**
     * Check for App Updates
     */
    override suspend fun checkForAppUpdates(): Boolean {
        val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
        if (canUpdate(appUpdateInfo)) {
            return suspendCancellableCoroutine { continuation ->
                val installStateUpdatedListener = getInstallStateListener(continuation)

                appUpdateManager.registerListener(installStateUpdatedListener)

                continuation.invokeOnCancellation {
                    appUpdateManager.unregisterListener(installStateUpdatedListener)
                    updateFlowResultLauncher?.unregister()
                }

                startUpdateFlow(appUpdateInfo, getIntentSenderStarter())
            }
        }
        return false
    }

    private fun canUpdate(appUpdateInfo: AppUpdateInfo) =
        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
            AppUpdateType.FLEXIBLE
        )


    private fun getInstallStateListener(continuation: CancellableContinuation<Boolean>) =
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    Timber.d("InAppUpdate: The user has downloaded the update")
                    continuation.resumeWith(Result.success(true))
                }

                InstallStatus.FAILED -> {
                    Timber.d("InAppUpdate: update failed to download")
                    continuation.resumeWith(Result.success(false))
                }

                InstallStatus.CANCELED -> {
                    Timber.d("InAppUpdate: The user has canceled downloading the update")
                    continuation.resumeWith(Result.success(false))
                }

                else -> {}
            }
        }

    private fun getIntentSenderStarter() =
        IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
            val request =
                IntentSenderRequest.Builder(intent).setFillInIntent(fillInIntent)
                    .setFlags(flagsValues, flagsMask).build()
            updateFlowResultLauncher?.launch(request)
        }

    private fun startUpdateFlow(
        appUpdateInfo: AppUpdateInfo,
        intentSenderForResultStarter: IntentSenderForResultStarter,
    ) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo, intentSenderForResultStarter, AppUpdateOptions.newBuilder(
                AppUpdateType.FLEXIBLE
            ).build(), appUpdateRequestCode
        )
    }

    /**
     * Complete the update
     */
    override fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
}