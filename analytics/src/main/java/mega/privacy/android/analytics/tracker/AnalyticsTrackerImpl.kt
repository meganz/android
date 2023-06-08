package mega.privacy.android.analytics.tracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.event.ButtonInfo
import mega.privacy.android.analytics.event.DialogInfo
import mega.privacy.android.analytics.event.NavigationInfo
import mega.privacy.android.analytics.event.NotificationInfo
import mega.privacy.android.analytics.event.ScreenInfo
import mega.privacy.android.analytics.event.TabInfo
import mega.privacy.android.domain.entity.analytics.AnalyticsEvent
import mega.privacy.android.domain.entity.analytics.ButtonPressedEvent
import mega.privacy.android.domain.entity.analytics.DialogDisplayedEvent
import mega.privacy.android.domain.entity.analytics.NavigationEvent
import mega.privacy.android.domain.entity.analytics.NotificationEvent
import mega.privacy.android.domain.entity.analytics.TabSelectedEvent
import mega.privacy.android.domain.entity.analytics.identifier.ButtonPressedEventIdentifier
import mega.privacy.android.domain.entity.analytics.identifier.DialogDisplayedEventIdentifier
import mega.privacy.android.domain.entity.analytics.identifier.NavigationEventIdentifier
import mega.privacy.android.domain.entity.analytics.identifier.NotificationEventIdentifier
import mega.privacy.android.domain.entity.analytics.identifier.ScreenViewEventIdentifier
import mega.privacy.android.domain.entity.analytics.identifier.TabSelectedEventIdentifier
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.analytics.GetViewIdUseCase
import mega.privacy.android.domain.usecase.analytics.TrackEventUseCase
import mega.privacy.android.domain.usecase.analytics.TrackScreenViewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Analytics tracker impl
 *
 * @property appScope
 * @property trackScreenViewUseCase
 */
class AnalyticsTrackerImpl @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val trackScreenViewUseCase: TrackScreenViewUseCase,
    private val trackEventUseCase: TrackEventUseCase,
    private val getViewIdUseCase: GetViewIdUseCase,
) : AnalyticsTracker {

    @Volatile
    private var currentViewId: String? = null

    private val eventSource =
        Channel<AnalyticsEvent>(capacity = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST) {
            Timber.w("Unable to deliver analytics event $it due to buffer overflow")
        }

    init {
        appScope.launch {
            eventSource.consumeAsFlow()
                .distinctUntilChanged()
                .collect {
                    trackEventUseCase(it)
                }

        }
    }

    override fun trackScreenView(screen: ScreenInfo) {
        appScope.launch {
            val identifier = ScreenViewEventIdentifier(
                name = screen.name,
                uniqueIdentifier = screen.uniqueIdentifier
            )
            val latestViewId = currentViewId
            trackScreenViewUseCase(identifier).also {
                synchronized(this@AnalyticsTrackerImpl) {
                    if (currentViewId == latestViewId) {
                        currentViewId = it
                    }
                }
            }
        }
    }

    override fun trackTabSelected(tab: TabInfo) {
        appScope.launch {

            val identifier = TabSelectedEventIdentifier(
                screenName = tab.screenInfo.name,
                tabName = tab.name,
                uniqueIdentifier = tab.uniqueIdentifier
            )

            if (currentViewId == null) {
                val newId = getViewIdUseCase()
                synchronized(this@AnalyticsTrackerImpl) {
                    if (currentViewId == null) {
                        currentViewId = newId
                    }
                }
            }
            val latestViewId = currentViewId

            if (latestViewId == null) {
                Timber.w("Tab selected analytics not sent due to null view ID")
            } else {
                eventSource.send(TabSelectedEvent(identifier, latestViewId))
            }
        }
    }

    override fun trackDialogDisplayed(dialog: DialogInfo) {
        trackDialog(dialog)
    }

    override fun trackButtonPress(button: ButtonInfo) {
        appScope.launch {
            val identifier = ButtonPressedEventIdentifier(
                buttonName = button.name,
                uniqueIdentifier = button.uniqueIdentifier,
                screenName = button.screen?.name,
                dialogName = button.dialog?.name,
            )
            trackEventUseCase(ButtonPressedEvent(identifier, currentViewId))
        }
    }

    override fun trackNavigation(navigation: NavigationInfo) {
        appScope.launch {
            val identifier = NavigationEventIdentifier(
                uniqueIdentifier = navigation.uniqueIdentifier,
                navigationElementType = navigation.source.name.lowercase(),
                destination = navigation.destination
            )

            trackEventUseCase(NavigationEvent(identifier, currentViewId))
        }
    }

    override fun trackNotification(notification: NotificationInfo) {
        appScope.launch {
            val identifier = NotificationEventIdentifier(
                name = notification.notificationName,
                uniqueIdentifier = notification.uniqueIdentifier
            )

            trackEventUseCase(NotificationEvent(identifier))
        }
    }

    override fun trackDialogDisplayed(dialog: DialogInfo, screen: ScreenInfo) {
        trackDialog(dialog, screen)
    }

    private fun trackDialog(dialog: DialogInfo, screen: ScreenInfo? = null) {
        appScope.launch {
            val identifier = DialogDisplayedEventIdentifier(
                dialogName = dialog.name,
                uniqueIdentifier = dialog.uniqueIdentifier,
                screenName = screen?.name
            )
            trackEventUseCase(DialogDisplayedEvent(identifier, currentViewId))
        }
    }

}