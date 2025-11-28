package mega.privacy.android.app.presentation.snackbar

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.core.sharedcomponents.snackbar.MegaSnackbarDuration
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is to handle snackbar message on activity
 * @property activityLifecycleHandler [ActivityLifecycleHandler]
 * @property applicationScope [CoroutineScope]
 * @property mainDispatcher [CoroutineDispatcher]
 * @property context [Context]
 */
@Singleton
class SnackBarHandlerImpl @Inject constructor(
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
) : SnackBarHandler {
    private val snackBarDecoration = MutableSharedFlow<SnackBarDecoration>()
    private var isSingleActivityEnabled = MutableStateFlow<Boolean?>(null)

    init {
        getFeatureEnabledValue()
        monitorSnackbarDecoration()
    }

    private fun getFeatureEnabledValue() {
        applicationScope.launch {
            isSingleActivityEnabled.update {
                getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
            }
        }
    }

    private fun monitorSnackbarDecoration() {
        applicationScope.launch(mainDispatcher) {
            snackBarDecoration.collect { snackBarDecoration ->
                while (activityLifecycleHandler.getCurrentActivity() == null) {
                    // Wait for an activity to be created
                    delay(100L)
                }
                activityLifecycleHandler.getCurrentActivity()?.let { activity ->
                    if (activity is MegaSnackbarShower) {
                        activity.showMegaSnackbar(
                            message = snackBarDecoration.message,
                            actionLabel = snackBarDecoration.actionLabel,
                            duration = snackBarDecoration.snackbarDuration
                        )
                    } else if (activity is SnackbarShower) {
                        activity.showSnackbar(content = snackBarDecoration.message)
                    }
                }
            }
        }
    }

    /**
     * Post message to show snackbar
     *
     * @param message text to be shown in the Snackbar
     * @param actionLabel optional action label to show as button in the Snackbar
     * @param snackbarDuration duration to control how long snackbar will be shown in [SnackbarHost], either
     * [SnackbarDuration.Short], [SnackbarDuration.Long] or [SnackbarDuration.Indefinite]
     */
    override fun postSnackbarMessage(
        message: String,
        actionLabel: String?,
        snackbarDuration: MegaSnackbarDuration,
    ) {
        applicationScope.launch {
            postSnackbarMessageInternal(message, actionLabel, snackbarDuration)
        }
    }

    /**
     * Post message to show snackbar
     *
     * @param resId ResourceId from strings
     * @param actionLabel optional action label to show as button in the Snackbar
     * @param snackbarDuration duration to control how long snackbar will be shown in [SnackbarHost], either
     * [SnackbarDuration.Short], [SnackbarDuration.Long] or [SnackbarDuration.Indefinite]
     */
    override fun postSnackbarMessage(
        @StringRes resId: Int,
        actionLabel: String?,
        snackbarDuration: MegaSnackbarDuration,
    ) {
        applicationScope.launch {
            val message = context.getString(resId)
            postSnackbarMessageInternal(message, actionLabel, snackbarDuration)
        }
    }

    /**
     * Internal method to handle snackbar message posting with unified logic.
     * This method handles the feature flag logic to either queue message for single activity mode
     * or emit decoration for legacy activity mode.
     *
     * @param message text to be shown in the Snackbar
     * @param actionLabel optional action label to show as button in the Snackbar
     * @param snackbarDuration duration to control how long snackbar will be shown in [SnackbarHost], either
     * [SnackbarDuration.Short], [SnackbarDuration.Long] or [SnackbarDuration.Indefinite]
     */
    private suspend fun postSnackbarMessageInternal(
        message: String,
        actionLabel: String?,
        snackbarDuration: MegaSnackbarDuration,
    ) {
        val snackBarDecorationValue = SnackBarDecoration(
            message = message,
            actionLabel = actionLabel,
            snackbarDuration = snackbarDuration
        )

        // Await for the feature flag to be evaluated
        if (isSingleActivityEnabled.first { it != null } == true) {
            snackbarEventQueue.queueMessage(
                SnackbarAttributes(
                    message = message,
                    action = actionLabel,
                    duration = mapDuration(snackbarDuration)
                )
            )
        } else {
            snackBarDecoration.emit(snackBarDecorationValue)
        }
    }

    /**
     * Maps MegaSnackbarDuration to SnackbarDuration
     */
    private fun mapDuration(snackbarDuration: MegaSnackbarDuration): SnackbarDuration =
        when (snackbarDuration) {
            MegaSnackbarDuration.Short -> SnackbarDuration.Short
            MegaSnackbarDuration.Long -> SnackbarDuration.Long
            MegaSnackbarDuration.Indefinite -> SnackbarDuration.Indefinite
        }
}

private data class SnackBarDecoration(
    val message: String,
    val actionLabel: String? = null,
    val snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
)
