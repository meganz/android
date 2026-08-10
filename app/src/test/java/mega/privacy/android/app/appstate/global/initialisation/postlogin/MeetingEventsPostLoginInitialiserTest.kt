package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.navigation.FreePlanParticipantsLimitNavKey
import mega.privacy.android.app.presentation.meeting.navigation.UpgradeProPlanBottomSheetNavKey
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MeetingEventsPostLoginInitialiserTest {
    private lateinit var underTest: MeetingEventsPostLoginInitialiser

    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()

    @BeforeAll
    fun setUp() {
        underTest = MeetingEventsPostLoginInitialiser(
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            navigationEventQueue = navigationEventQueue,
            appDialogsEventQueue = appDialogsEventQueue
        )
    }

    @AfterEach
    fun resetMock() {
        reset(
            monitorChatCallUpdatesUseCase,
            navigationEventQueue,
            appDialogsEventQueue
        )
    }

    @Test
    fun `test that FreePlanParticipantsLimitNavKey is emitted when CallUsersLimit term code and conditions are met`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.TerminatingUserParticipation,
                    termCode = ChatCallTermCodeType.CallUsersLimit
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)
            underTest("session", false)
            verify(appDialogsEventQueue).emit(
                argThat<AppDialogEvent> { event ->
                    event.dialogDestination == FreePlanParticipantsLimitNavKey(
                        callEndedDueToFreePlanLimits = true
                    )
                },
                any()
            )
        }

    @Test
    fun `test that UpgradeProPlanBottomSheetNavKey is emitted when CallDurationLimit term code and conditions are met`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.TerminatingUserParticipation,
                    termCode = ChatCallTermCodeType.CallDurationLimit,
                    isOwnClientCaller = true
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)
            underTest("session", false)

            verify(navigationEventQueue).emit(UpgradeProPlanBottomSheetNavKey)
        }

    @Test
    fun `test that UpgradeProPlanBottomSheetNavKey is not emitted when not own client caller`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.TerminatingUserParticipation,
                    termCode = ChatCallTermCodeType.CallDurationLimit,
                    isOwnClientCaller = false
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)
            underTest("session", false)

            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that FreePlanParticipantsLimitNavKey is emitted for GenericNotification status with CallUsersLimit`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.GenericNotification,
                    termCode = ChatCallTermCodeType.CallUsersLimit
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)
            underTest("session", false)
            verify(appDialogsEventQueue).emit(
                argThat<AppDialogEvent> { event ->
                    event.dialogDestination == FreePlanParticipantsLimitNavKey(
                        callEndedDueToFreePlanLimits = true
                    )
                },
                any()
            )
        }

    @Test
    fun `test that no navigation event is emitted for other call statuses`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.InProgress,
                    termCode = ChatCallTermCodeType.CallUsersLimit
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)
            underTest("session", false)

            verifyNoInteractions(navigationEventQueue)
        }

    @Test
    fun `test that no navigation event is emitted for other term codes`() =
        runTest {
            val callFlow = flowOf(
                ChatCall(
                    chatId = 1L,
                    callId = 1L,
                    status = ChatCallStatus.TerminatingUserParticipation,
                    termCode = ChatCallTermCodeType.TooManyParticipants
                )
            )

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(callFlow)

            underTest("session", false)

            verifyNoInteractions(navigationEventQueue)
        }
}
