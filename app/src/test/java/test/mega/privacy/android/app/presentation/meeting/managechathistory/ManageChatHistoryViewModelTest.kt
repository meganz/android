package test.mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.presentation.meeting.model.newChatRoom
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageChatHistoryViewModelTest {

    private lateinit var underTest: ManageChatHistoryViewModel

    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val setChatRetentionTimeUseCase = mock<SetChatRetentionTimeUseCase>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val getContactHandleUseCase = mock<GetContactHandleUseCase>()
    private val getChatRoomByUserUseCase = mock<GetChatRoomByUserUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()

    private lateinit var savedStateHandle: SavedStateHandle

    private val chatRoomId = 123L
    private val email = "test@test.com"

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("CHAT_ROOM_ID_KEY" to null))
        initializeViewModel()
    }

    private fun initializeViewModel() {
        underTest = ManageChatHistoryViewModel(
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            setChatRetentionTimeUseCase = setChatRetentionTimeUseCase,
            getChatRoomUseCase = getChatRoomUseCase,
            getContactHandleUseCase = getContactHandleUseCase,
            getChatRoomByUserUseCase = getChatRoomByUserUseCase,
            savedStateHandle = savedStateHandle,
            snackBarHandler = snackBarHandler
        )
    }

    @AfterEach
    fun resetMocks() {
        wheneverBlocking { monitorChatRetentionTimeUpdateUseCase(chatRoomId) } doReturn emptyFlow()
        reset(
            monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase,
            snackBarHandler
        )
    }

    @Test
    fun `test that retention time in state is updated when retention time update is received`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom(withChatId = chatRoomId)
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = email)

            underTest.retentionTimeUiState.test {
                assertThat(awaitItem()).isEqualTo(retentionTime)
            }

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().selectedHistoryRetentionTimeOption
                ).isEqualTo(
                    ChatHistoryRetentionOption.Custom
                )
            }
        }

    @Test
    fun `test that the chat's history is cleared with the correct chat room ID`() = runTest {
        underTest.clearChatHistory(chatRoomId)

        verify(clearChatHistoryUseCase).invoke(chatRoomId)
    }

    @Test
    fun `test that the clear chat history visibility state is true`() = runTest {
        underTest.showClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isTrue()
        }
    }

    @Test
    fun `test that the clear chat history visibility state is false when dismissed`() = runTest {
        underTest.showClearChatConfirmation()
        underTest.dismissClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isFalse()
        }
    }

    @Test
    fun `test that the correct snack bar message is shown after successfully clearing the chat history`() =
        runTest {
            underTest.clearChatHistory(chatRoomId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_success,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that the correct snack bar message is shown when clearing the chat history fails`() =
        runTest {
            whenever(clearChatHistoryUseCase(chatRoomId)) doThrow RuntimeException()

            underTest.clearChatHistory(chatRoomId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_error,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that chat's retention time updated with the correct parameters`() =
        runTest {
            val period = 321L
            underTest.setChatRetentionTime(period = period)

            verify(setChatRetentionTimeUseCase).invoke(any(), eq(period))
        }

    @ParameterizedTest
    @ValueSource(longs = [123L])
    @NullSource
    fun `test that the chat room id should be set when the chat room is initialized`(chatRoomId: Long?) =
        runTest {
            val newChatRoomId = chatRoomId ?: MEGACHAT_INVALID_HANDLE
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(newChatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom(withChatId = newChatRoomId)
            whenever(getChatRoomUseCase(newChatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = null)

            val actual = savedStateHandle.get<Long>("CHAT_ROOM_ID_KEY") ?: MEGACHAT_INVALID_HANDLE
            assertThat(actual).isEqualTo(newChatRoomId)
        }

    @Test
    fun `test that the chat room UI state is updated after successfully retrieving the chat room with a valid handle`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom(withChatId = chatRoomId)
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = chatRoomId, email = null)

            underTest.chatRoomUiState.test {
                assertThat(expectMostRecentItem()).isEqualTo(chatRoom)
            }
        }

    @ParameterizedTest
    @MethodSource("provideEmailAndContactHandle")
    fun `test that the screen is navigated up`(email: String?, contactHandle: Long?) =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            email?.let {
                whenever(getContactHandleUseCase(it)) doReturn contactHandle
            }

            underTest.initializeChatRoom(chatId = null, email = email)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldNavigateUp).isTrue()
            }
        }

    private fun provideEmailAndContactHandle() = Stream.of(
        Arguments.of(null, 123L),
        Arguments.of("    ", 123L),
        Arguments.of(email, null),
    )

    @Test
    fun `test that the chat room UI state is updated after successfully retrieving the chat room by the user`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val contactHandle = 321L
            whenever(getContactHandleUseCase(email)) doReturn contactHandle
            val chatRoom = newChatRoom(withChatId = chatRoomId)
            whenever(getChatRoomByUserUseCase(contactHandle)) doReturn chatRoom

            underTest.initializeChatRoom(chatId = null, email = email)

            underTest.chatRoomUiState.test {
                assertThat(expectMostRecentItem()).isEqualTo(chatRoom)
            }
        }

    @Test
    fun `test that the navigate up UI state is reset after navigating up`() =
        runTest {
            underTest.initializeChatRoom(chatId = null, email = null)
            underTest.onNavigatedUp()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldNavigateUp).isFalse()
            }
        }

    @Test
    fun `test that the retention time UI state is updated after successfully retrieving the chat room`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
                retentionTime
            )
            val chatRoom = newChatRoom(withChatId = chatRoomId, withRetentionTime = retentionTime)
            whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

            savedStateHandle = SavedStateHandle(mapOf("CHAT_ROOM_ID_KEY" to chatRoomId))
            initializeViewModel()
            underTest.initializeChatRoom(chatId = chatRoomId, email = null)

            underTest.retentionTimeUiState.test {
                assertThat(expectMostRecentItem()).isEqualTo(retentionTime)
            }
        }

    @ParameterizedTest
    @MethodSource("provideRetentionTimePeriod")
    fun `test that the selected chat history retention time should be set with the correct option`(
        period: Long,
        expectedOption: ChatHistoryRetentionOption,
    ) = runTest {
        savedStateHandle = SavedStateHandle(mapOf("CHAT_ROOM_ID_KEY" to chatRoomId))
        initializeViewModel()
        underTest.setChatRetentionTime(period)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().selectedHistoryRetentionTimeOption
            ).isEqualTo(
                expectedOption
            )
        }
    }

    private fun provideRetentionTimePeriod() = Stream.of(
        Arguments.of(
            DISABLED_RETENTION_TIME,
            ChatHistoryRetentionOption.Disabled
        ),
        Arguments.of(
            SECONDS_IN_DAY,
            ChatHistoryRetentionOption.OneDay
        ),
        Arguments.of(
            SECONDS_IN_WEEK,
            ChatHistoryRetentionOption.OneWeek
        ),
        Arguments.of(
            SECONDS_IN_MONTH_30,
            ChatHistoryRetentionOption.OneMonth
        ),
        Arguments.of(
            100L,
            ChatHistoryRetentionOption.Custom
        )
    )

    @Test
    fun `test that the custom time picker is displayed when the confirmed option is set to custom`() =
        runTest {
            underTest.onNewRetentionTimeConfirmed(ChatHistoryRetentionOption.Custom)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowCustomTimePicker).isTrue()
            }
        }

    @ParameterizedTest
    @MethodSource("provideConfirmedRetentionTimeOption")
    fun `test that the retention time is updated with the correct period when the confirmed option is not custom`(
        option: ChatHistoryRetentionOption,
        expectedPeriod: Long,
    ) = runTest {
        underTest.onNewRetentionTimeConfirmed(option)

        verify(setChatRetentionTimeUseCase).invoke(any(), eq(expectedPeriod))
    }

    private fun provideConfirmedRetentionTimeOption() = Stream.of(
        Arguments.of(
            ChatHistoryRetentionOption.Disabled,
            DISABLED_RETENTION_TIME
        ),
        Arguments.of(
            ChatHistoryRetentionOption.OneDay,
            SECONDS_IN_DAY.toLong()
        ),
        Arguments.of(
            ChatHistoryRetentionOption.OneWeek,
            SECONDS_IN_WEEK.toLong()
        ),
        Arguments.of(
            ChatHistoryRetentionOption.OneMonth,
            SECONDS_IN_MONTH_30.toLong()
        )
    )

    @Test
    fun `test that the custom picker is not displayed after being set`() = runTest {
        underTest.onCustomTimePickerSet()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowCustomTimePicker).isFalse()
        }
    }

    @Test
    fun `test that the history retention confirmation is displayed`() = runTest {
        underTest.showHistoryRetentionConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowHistoryRetentionConfirmation).isTrue()
        }
    }

    @Test
    fun `test that the history retention confirmation is not displayed when dismissed`() = runTest {
        underTest.showHistoryRetentionConfirmation()
        underTest.dismissHistoryRetentionConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowHistoryRetentionConfirmation).isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(ChatHistoryRetentionOption::class)
    fun `test that the correct string resource id is set when updating the history retention time confirmation`(
        option: ChatHistoryRetentionOption,
    ) = runTest {
        underTest.updateHistoryRetentionTimeConfirmation(option)

        underTest.uiState.test {
            val expected = if (option == ChatHistoryRetentionOption.Custom) {
                R.string.general_next
            } else {
                R.string.general_ok
            }
            assertThat(expectMostRecentItem().confirmButtonStringId).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @MethodSource("provideRetentionTimeAndOptionWithExpectedValue")
    fun `test that the correct enablement value for the confirm button is set when updating the history retention time confirmation`(
        retentionTime: Long,
        option: ChatHistoryRetentionOption,
        expected: Boolean,
    ) = runTest {
        whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
            retentionTime
        )
        val chatRoom = newChatRoom(withChatId = chatRoomId, withRetentionTime = retentionTime)
        whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

        underTest.initializeChatRoom(chatId = chatRoomId, email = null)
        underTest.updateHistoryRetentionTimeConfirmation(option)

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isConfirmButtonEnable).isEqualTo(expected)
        }
    }

    private fun provideRetentionTimeAndOptionWithExpectedValue() = Stream.of(
        Arguments.of(
            DISABLED_RETENTION_TIME,
            ChatHistoryRetentionOption.Disabled,
            false
        ),
        Arguments.of(
            DISABLED_RETENTION_TIME,
            ChatHistoryRetentionOption.OneMonth,
            true
        ),
        Arguments.of(
            SECONDS_IN_DAY,
            ChatHistoryRetentionOption.Disabled,
            true
        )
    )

    @ParameterizedTest
    @ValueSource(longs = [100L, DISABLED_RETENTION_TIME])
    fun `test that the correct string resource id is set when showing the history retention time confirmation`(
        retentionTime: Long,
    ) = runTest {
        whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
            retentionTime
        )
        val chatRoom = newChatRoom(withChatId = chatRoomId, withRetentionTime = retentionTime)
        whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

        underTest.initializeChatRoom(chatId = chatRoomId, email = null)
        underTest.showHistoryRetentionConfirmation()

        underTest.uiState.test {
            val expected = if (retentionTime == 100L) {
                R.string.general_next
            } else {
                R.string.general_ok
            }
            assertThat(expectMostRecentItem().confirmButtonStringId).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [DISABLED_RETENTION_TIME, SECONDS_IN_DAY.toLong()])
    fun `test that the correct enablement value for the confirm button is set when showing the history retention time confirmation`(
        retentionTime: Long,
    ) = runTest {
        whenever(monitorChatRetentionTimeUpdateUseCase(chatRoomId)) doReturn flowOf(
            retentionTime
        )
        val chatRoom = newChatRoom(withChatId = chatRoomId, withRetentionTime = retentionTime)
        whenever(getChatRoomUseCase(chatRoomId)) doReturn chatRoom

        underTest.initializeChatRoom(chatId = chatRoomId, email = null)
        underTest.showHistoryRetentionConfirmation()

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().isConfirmButtonEnable
            ).isEqualTo(
                retentionTime != DISABLED_RETENTION_TIME
            )
        }
    }
}
