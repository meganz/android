package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.retry
import mega.privacy.android.app.presentation.meeting.navigation.ForceAppUpdateNavKey
import mega.privacy.android.app.presentation.meeting.navigation.FreePlanParticipantsLimitNavKey
import mega.privacy.android.app.presentation.meeting.navigation.UpgradeProPlanBottomSheetNavKey
import mega.privacy.android.domain.entity.call.CallNotificationType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initialiser that monitors chat call updates and triggers navigation
 * for free plan participants limit dialog, upgrade to Pro plan bottom sheet and
 * force app update dialog
 */
class MeetingEventsPostLoginInitialiser @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val navigationEventQueue: NavigationEventQueue,
    private val appDialogsEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        monitorChatCallUpdatesUseCase()
            .retry {
                Timber.e(it, "Error in monitoring meeting events")
                true
            }.collect { call ->
                if (!call.reportsTermCode()) return@collect

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

                    ChatCallTermCodeType.ProtocolVersion -> {
                        Timber.d("Emitting ForceAppUpdateNavKey")
                        appDialogsEventQueue.emit(AppDialogEvent(ForceAppUpdateNavKey))
                    }

                    else -> Unit
                }
            }
    }
)

/**
 * A term code is meaningful either when the SFU ends our participation in the call, or when it
 * reports the term code as an SFU error carried by a generic notification while the call is still
 * running. Any other notification type is ignored, since acting on one can close a live call.
 */
private fun ChatCall.reportsTermCode() =
    status == ChatCallStatus.TerminatingUserParticipation
            || (changes?.contains(ChatCallChanges.GenericNotification) == true
            && notificationType == CallNotificationType.SFUError)
