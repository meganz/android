package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.retry
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.presentation.meeting.navigation.FreePlanParticipantsLimitNavKey
import mega.privacy.android.app.presentation.meeting.navigation.UpgradeProPlanBottomSheetNavKey
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initialiser that monitors chat call updates and triggers navigation
 * for free plan participants limit dialog and upgrade to Pro plan bottom sheet
 */
class MeetingEventsPostLoginInitialiser @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val navigationEventQueue: NavigationEventQueue,
    private val appDialogsEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        monitorChatCallUpdatesUseCase()
            .retry {
                Timber.e(it, "Error in monitoring meeting events")
                true
            }.collect { call ->
                when (call.status) {
                    ChatCallStatus.TerminatingUserParticipation,
                    ChatCallStatus.GenericNotification,
                        -> {
                        when (call.termCode) {
                            ChatCallTermCodeType.CallUsersLimit -> {
                                Timber.d("Emitting FreePlanParticipantsLimitNavKey")
                                appDialogsEventQueue.emit(
                                    AppDialogEvent(
                                        FreePlanParticipantsLimitNavKey(
                                            callEndedDueToFreePlanLimits = true
                                        )
                                    )
                                )
                            }

                            ChatCallTermCodeType.CallDurationLimit -> {
                                if (call.isOwnClientCaller) {
                                    Timber.d("Emitting UpgradeProPlanBottomSheetNavKey")
                                    navigationEventQueue.emit(UpgradeProPlanBottomSheetNavKey)
                                }
                            }

                            else -> {
                                // No action for other term codes
                            }
                        }
                    }

                    else -> {
                        // No action for other statuses
                    }
                }
            }
    }
)
