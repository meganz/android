package mega.privacy.android.app.presentation.chat.list.dialog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.usecase.chat.MuteChatPushNotificationUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MuteDialogViewModelTest {

    private lateinit var underTest: MuteDialogViewModel

    private val muteChatPushNotificationUseCase: MuteChatPushNotificationUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = MuteDialogViewModel(
            muteChatPushNotificationUseCase = muteChatPushNotificationUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(muteChatPushNotificationUseCase)
    }

    @Test
    fun `test that muteOptionsFor returns timed options plus a morning tail for Global target`() {
        val options = underTest.muteOptionsFor(MuteTarget.Global)

        assertThat(options).hasSize(5)
        assertThat(options.dropLast(1)).containsExactly(
            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
        ).inOrder()
        assertThat(options.last()).isAnyOf(
            ChatPushNotificationMuteOption.MuteUntilThisMorning,
            ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
        )
    }

    @Test
    fun `test that muteOptionsFor returns timed options plus MuteUntilTurnBackOn for Single target`() {
        val options = underTest.muteOptionsFor(MuteTarget.Single(chatId = 42L, isMeeting = false))

        assertThat(options).containsExactly(
            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
            ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
        ).inOrder()
    }

    @Test
    fun `test that muteOptionsFor returns timed options plus MuteUntilTurnBackOn for Multiple target`() {
        val options = underTest.muteOptionsFor(
            MuteTarget.Multiple(chatIds = listOf(1L, 2L), isMeeting = true)
        )

        assertThat(options).containsExactly(
            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
            ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
        ).inOrder()
    }

    @Test
    fun `test that applyMute invokes the use case with null chatIds when target is Global`() =
        runTest {
            underTest.applyMute(
                target = MuteTarget.Global,
                option = ChatPushNotificationMuteOption.Mute1Hour,
            )

            verify(muteChatPushNotificationUseCase).invoke(
                null,
                ChatPushNotificationMuteOption.Mute1Hour,
            )
        }

    @Test
    fun `test that applyMute invokes the use case with a single-element chatIds list when target is Single`() =
        runTest {
            underTest.applyMute(
                target = MuteTarget.Single(chatId = 42L, isMeeting = false),
                option = ChatPushNotificationMuteOption.Mute30Minutes,
            )

            verify(muteChatPushNotificationUseCase).invoke(
                listOf(42L),
                ChatPushNotificationMuteOption.Mute30Minutes,
            )
        }

    @Test
    fun `test that applyMute invokes the use case with the full chatIds list when target is Multiple`() =
        runTest {
            val chatIds = listOf(1L, 2L, 3L)

            underTest.applyMute(
                target = MuteTarget.Multiple(chatIds = chatIds, isMeeting = true),
                option = ChatPushNotificationMuteOption.Mute24Hours,
            )

            verify(muteChatPushNotificationUseCase).invoke(
                chatIds,
                ChatPushNotificationMuteOption.Mute24Hours,
            )
        }

    @Test
    fun `test that muteResultEvent starts consumed`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().muteResultEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that muteResultEvent is triggered with the applied option once the use case succeeds`() =
        runTest {
            underTest.uiState.test {
                assertThat(awaitItem().muteResultEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)

                underTest.applyMute(
                    target = MuteTarget.Single(chatId = 1L, isMeeting = false),
                    option = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
                )

                val triggered = awaitItem().muteResultEvent
                assertThat(triggered)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((triggered as StateEventWithContentTriggered).content)
                    .isEqualTo(ChatPushNotificationMuteOption.MuteUntilTurnBackOn)
            }
        }

    @Test
    fun `test that onMuteResultEventConsumed clears the muteResultEvent`() = runTest {
        underTest.applyMute(
            target = MuteTarget.Global,
            option = ChatPushNotificationMuteOption.Mute1Hour,
        )

        underTest.uiState.test {
            val first = awaitItem()
            if (first.muteResultEvent is StateEventWithContentConsumed) {
                assertThat(awaitItem().muteResultEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
            } else {
                assertThat(first.muteResultEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
            }

            underTest.onMuteResultEventConsumed()

            assertThat(awaitItem().muteResultEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }
}
