package mega.privacy.android.app.presentation.snackbar

import androidx.compose.material.SnackbarDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is to handle snackbar message on activity
 * @property activityLifecycleHandler [ActivityLifecycleHandler]
 * @property applicationScope [CoroutineScope]
 * @property mainDispatcher [CoroutineDispatcher]
 */
@Singleton
class SnackBarHandler @Inject constructor(
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {
    private val snackBarDecoration = MutableSharedFlow<SnackBarDecoration>()

    init {
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
     * @param duration duration to control how long snackbar will be shown in [SnackbarHost], either
     * [SnackbarDuration.Short], [SnackbarDuration.Long] or [SnackbarDuration.Indefinite]
     */
    fun postSnackbarMessage(
        message: String,
        actionLabel: String? = null,
        snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
    ) {
        applicationScope.launch {
            val snackBarDecorationValue = SnackBarDecoration(
                message = message,
                actionLabel = actionLabel,
                snackbarDuration = snackbarDuration
            )
            snackBarDecoration.emit(snackBarDecorationValue)
        }
    }
}

private data class SnackBarDecoration(
    val message: String,
    val actionLabel: String? = null,
    val snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
)
