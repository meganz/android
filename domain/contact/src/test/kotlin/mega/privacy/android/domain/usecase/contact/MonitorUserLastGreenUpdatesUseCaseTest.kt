package mega.privacy.android.domain.usecase.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserLastGreen
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUserLastGreenUpdatesUseCaseTest {

    private lateinit var underTest: MonitorUserLastGreenUpdatesUseCase

    private val monitorChatPresenceLastGreenUpdatesUseCase =
        mock<MonitorChatPresenceLastGreenUpdatesUseCase>()

    @BeforeEach
    fun setup() {
        underTest =
            MonitorUserLastGreenUpdatesUseCase(monitorChatPresenceLastGreenUpdatesUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(monitorChatPresenceLastGreenUpdatesUseCase)
    }

    @Test
    fun `test that monitor contact last green updates returns flow of last green`() = runTest {
        val userHandle = 123L
        whenever(monitorChatPresenceLastGreenUpdatesUseCase()).thenReturn(
            flowOf(
                UserLastGreen(handle = userHandle, lastGreen = 5),
                UserLastGreen(handle = 321L, lastGreen = 7)
            )
        )

        underTest(userHandle).test {
            val actual = awaitItem()
            awaitComplete()
            Truth.assertThat(actual).isEqualTo(5)
        }
    }
}