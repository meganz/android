package mega.privacy.android.feature.chat.meeting.recording.initialiser

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorRecordedChatsUseCase
import mega.privacy.android.feature.chat.navigation.CallRecordingConsentDialogNavKey
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallRecordingMonitoringInitialiserTest {
    private lateinit var underTest: CallRecordingMonitoringInitialiser

    private val monitorRecordedChatsUseCase = mock<MonitorRecordedChatsUseCase>()
    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()
    private val monitorCallRecordingConsentEventUseCase =
        mock<MonitorCallRecordingConsentEventUseCase>()
    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = CallRecordingMonitoringInitialiser(
            monitorRecordedChatsUseCase = monitorRecordedChatsUseCase,
            appDialogEventQueue = appDialogsEventQueue,
            monitorCallRecordingConsentEventUseCase = monitorCallRecordingConsentEventUseCase,
            broadcastCallRecordingConsentEventUseCase = broadcastCallRecordingConsentEventUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorRecordedChatsUseCase,
            appDialogsEventQueue,
            monitorCallRecordingConsentEventUseCase,
            broadcastCallRecordingConsentEventUseCase,
        )
    }

    @Test
    fun `test that a recording event and a consent event None emits a consent event pending and dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.None)
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verify(broadcastCallRecordingConsentEventUseCase).invoke(
                CallRecordingConsentStatus.Pending(
                    chatId
                )
            )

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Pending with matching id emits a dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Pending(chatId))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Pending with non-matching id emits a consent event pending and dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Pending(chatId - 1))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verify(broadcastCallRecordingConsentEventUseCase).invoke(
                CallRecordingConsentStatus.Pending(
                    chatId
                )
            )

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Requested with non-matching id emits a consent event pending and dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Requested(chatId - 1))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verify(broadcastCallRecordingConsentEventUseCase).invoke(
                CallRecordingConsentStatus.Pending(
                    chatId
                )
            )

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Requested with matching id emits nothing`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Requested(chatId))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)

            verifyNoInteractions(appDialogsEventQueue)

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Granted with non-matching id emits a consent event pending and dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Granted(chatId - 1))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verify(broadcastCallRecordingConsentEventUseCase).invoke(
                CallRecordingConsentStatus.Pending(
                    chatId
                )
            )

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Granted with matching id emits nothing`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Granted(chatId))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)

            verifyNoInteractions(appDialogsEventQueue)

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Denied with non-matching id emits a consent event pending and dialog event`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Denied(chatId - 1))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verify(broadcastCallRecordingConsentEventUseCase).invoke(
                CallRecordingConsentStatus.Pending(
                    chatId
                )
            )

            verify(appDialogsEventQueue).emit(
                argThat { this.dialogDestination == CallRecordingConsentDialogNavKey(chatId = chatId) },
                eq(NavPriority.Default)
            )

            job.cancelAndJoin()
        }

    @Test
    fun `test that a recording event and a consent event Denied with matching id emits nothing`() =
        runTest {
            val chatId = 12345L
            val participantRecording = "Bobby"

            monitorRecordedChatsUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingEvent.Recording(chatId, participantRecording))
                    awaitCancellation()
                }
            }

            monitorCallRecordingConsentEventUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(CallRecordingConsentStatus.Denied(chatId))
                    awaitCancellation()
                }
            }

            val job = launch {
                underTest.invoke("", false)
            }

            advanceUntilIdle()

            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)

            verifyNoInteractions(appDialogsEventQueue)

            job.cancelAndJoin()
        }


    @ParameterizedTest(name = "{0} with matching chat id emits correct consent event and no dialog event")
    @MethodSource("provideCallRecordingConsentParameters")
    fun `test that non-recording event with consent event`(
        status: CallRecordingConsentStatus,
        verification: suspend () -> Unit
    ) = runTest {
        val chatId = status.chatId

        monitorRecordedChatsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(CallRecordingEvent.RecordingEnded(chatId))
                awaitCancellation()
            }
        }

        monitorCallRecordingConsentEventUseCase.stub {
            on { invoke() } doReturn flow {
                emit(status)
                awaitCancellation()
            }
        }

        val job = launch {
            underTest.invoke("", false)
        }

        advanceUntilIdle()

        verification()

        verifyNoInteractions(appDialogsEventQueue)

        job.cancelAndJoin()
    }

    private fun provideCallRecordingConsentParameters(): List<Arguments> {
        val chatId = 123456L
        return listOf(
            Arguments.of(
                CallRecordingConsentStatus.None,
                suspend {
                    verifyNoInteractions(
                        broadcastCallRecordingConsentEventUseCase
                    )
                }),
            Arguments.of(
                CallRecordingConsentStatus.Pending(chatId),
                suspend {
                    verify(broadcastCallRecordingConsentEventUseCase).invoke(
                        CallRecordingConsentStatus.None
                    )
                }),
            Arguments.of(
                CallRecordingConsentStatus.Requested(chatId),
                suspend {
                    verify(broadcastCallRecordingConsentEventUseCase).invoke(
                        CallRecordingConsentStatus.None
                    )
                }),
            Arguments.of(
                CallRecordingConsentStatus.Granted(chatId),
                suspend {
                    verify(broadcastCallRecordingConsentEventUseCase).invoke(
                        CallRecordingConsentStatus.None
                    )
                }),
            Arguments.of(
                CallRecordingConsentStatus.Denied(chatId),
                suspend {
                    verify(broadcastCallRecordingConsentEventUseCase).invoke(
                        CallRecordingConsentStatus.None
                    )
                }),
        )
    }
}