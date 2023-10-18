package mega.privacy.android.app.service.inappupdate

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.middlelayer.inappupdate.InAppUpdateHandler
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.inappupdate.ResetInAppUpdateStatisticsUseCase
import mega.privacy.android.domain.usecase.inappupdate.ShouldPromptUserForUpdateUseCase
import mega.privacy.android.domain.usecase.inappupdate.ShouldResetInAppUpdateStatisticsUseCase
import mega.privacy.android.domain.usecase.inappupdate.UpdateInAppUpdateStatisticsUseCase
import mega.privacy.mobile.analytics.event.InAppUpdateCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.InAppUpdateDownloadSuccessMessageDisplayedEvent
import mega.privacy.mobile.analytics.event.InAppUpdateRestartButtonPressedEvent
import mega.privacy.mobile.analytics.event.InAppUpdateUpdateButtonPressedEvent
import org.jetbrains.anko.contentView
import timber.log.Timber
import javax.inject.Inject

/**
 * [InAppUpdateHandler] Implementation
 */
class InAppUpdateHandlerImpl @Inject constructor(
    @ActivityContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val resetInAppUpdateStatisticsUseCase: ResetInAppUpdateStatisticsUseCase,
    private val updateInAppUpdateStatisticsUseCase: UpdateInAppUpdateStatisticsUseCase,
    private val shouldPromptUserForUpdateUseCase: ShouldPromptUserForUpdateUseCase,
    private val shouldResetInAppUpdateStatisticsUseCase: ShouldResetInAppUpdateStatisticsUseCase,
) : InAppUpdateHandler {
    private val appUpdateManager: AppUpdateManager by lazy(LazyThreadSafetyMode.NONE) {
        AppUpdateManagerFactory.create(
            context
        )
    }
    private val appUpdateRequestCode = 123
    private val noOfDaysBeforePrompt = 7 //x
    private val incrementalFrequencyInDays = 10 //n
    private val incrementalPromptStopCount = 4 // This will stop prompts after x + 3n
    private val initialMessageDuration = 5000
    private val reminderMessageDuration = 1500
    private val isIncrementalPromptEnabled = false

    private var availableVersionCode = 0

    private val updateFlowResultLauncher: ActivityResultLauncher<IntentSenderRequest>? =
        (context as? ComponentActivity)?.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    updateInAppUpdateStatistics(neverShowAgain = false)
                    Analytics.tracker.trackEvent(InAppUpdateUpdateButtonPressedEvent)
                    Timber.d("InAppUpdate: The user has accepted the update")
                }


                AppCompatActivity.RESULT_CANCELED -> {
                    updateInAppUpdateStatistics(neverShowAgain = true)
                    Analytics.tracker.trackEvent(InAppUpdateCancelButtonPressedEvent)
                    Timber.d("InAppUpdate: The user has denied or canceled the update.")
                }

                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Timber.d("InAppUpdate: Unknown error in launching update flow")
                }
            }
        }

    private fun updateInAppUpdateStatistics(neverShowAgain: Boolean) {
        applicationScope.launch {
            updateInAppUpdateStatisticsUseCase(neverShowAgain, availableVersionCode)
        }
    }

    /**
     * Check for App Updates
     */
    override suspend fun checkForAppUpdates() {
        val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
        if (shouldResetInAppUpdateStatisticsUseCase(appUpdateInfo.availableVersionCode())) {
            resetInAppUpdateStatisticsUseCase()
        }
        if (shouldPromptUserForUpdate()) {
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
        }
    }

    override suspend fun checkForInAppUpdateInstallStatus() {
        val appUpdateInfo = appUpdateManager.requestAppUpdateInfo()
        // If the update is downloaded but not installed, notify the user to complete the update.
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackBarForCompleteUpdate(reminderMessageDuration)
        }
    }

    private fun canUpdate(appUpdateInfo: AppUpdateInfo) =
        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
            AppUpdateType.FLEXIBLE
        )


    private fun getInstallStateListener(continuation: CancellableContinuation<Unit>) =
        InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    continuation.resumeWith(Result.success(Unit))
                    Timber.d("InAppUpdate: The user has downloaded the update")
                    popupSnackBarForCompleteUpdate(initialMessageDuration)
                }

                InstallStatus.FAILED -> {
                    continuation.resumeWith(Result.success(Unit))
                    Timber.d("InAppUpdate: update failed to download")
                }

                InstallStatus.CANCELED -> {
                    continuation.resumeWith(Result.success(Unit))
                    Timber.d("InAppUpdate: The user has canceled downloading the update")
                }

                else -> {}
            }
        }

    private fun popupSnackBarForCompleteUpdate(duration: Int) {
        (context as? Activity)?.contentView?.let { view ->
            Snackbar.make(
                view,
                context.getString(R.string.general_app_update_message_download_success),
                duration
            ).apply {
                setAction(context.getString(R.string.general_app_update_action_restart)) {
                    Analytics.tracker.trackEvent(InAppUpdateRestartButtonPressedEvent)
                    completeUpdate()
                }
                show()
            }
            Analytics.tracker.trackEvent(InAppUpdateDownloadSuccessMessageDisplayedEvent)
        }
    }

    private fun getIntentSenderStarter() =
        IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
            val request = IntentSenderRequest.Builder(intent).setFillInIntent(fillInIntent)
                .setFlags(flagsValues, flagsMask).build()
            updateFlowResultLauncher?.launch(request)
        }

    private fun startUpdateFlow(
        appUpdateInfo: AppUpdateInfo,
        intentSenderForResultStarter: IntentSenderForResultStarter,
    ) {
        availableVersionCode = appUpdateInfo.availableVersionCode()
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

    /**
     * Control the frequency at which user is prompted
     */
    private suspend fun shouldPromptUserForUpdate(): Boolean {
        return shouldPromptUserForUpdateUseCase(
            noOfDaysBeforePrompt,
            isIncrementalPromptEnabled,
            incrementalFrequencyInDays,
            incrementalPromptStopCount
        )
    }
}