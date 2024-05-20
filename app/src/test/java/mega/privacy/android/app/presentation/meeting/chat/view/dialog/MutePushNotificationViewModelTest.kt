package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.usecase.chat.GetChatMuteOptionListUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class MutePushNotificationViewModelTest {

    private lateinit var underTest: MutePushNotificationViewModel
    private val getChatMuteOptionListUseCase: GetChatMuteOptionListUseCase = mock()


    fun initUnderTest() {
        underTest = MutePushNotificationViewModel(getChatMuteOptionListUseCase)
    }

    @Test
    fun `loadValues should update state with chat mute options`() = runTest {
        val expected = ChatPushNotificationMuteOption.entries
        whenever(getChatMuteOptionListUseCase()).thenReturn(expected)

        initUnderTest()

        underTest.mutePushNotificationUiState.test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `loadValues should update state with empty list when use case fails`() = runTest {
        whenever(getChatMuteOptionListUseCase()).thenAnswer { throw RuntimeException() }

        initUnderTest()

        underTest.mutePushNotificationUiState.test {
            assertThat(awaitItem()).isEmpty()
        }
    }
}