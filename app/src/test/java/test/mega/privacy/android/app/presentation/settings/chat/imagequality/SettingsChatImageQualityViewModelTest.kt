package test.mega.privacy.android.app.presentation.settings.chat.imagequality

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.chat.imagequality.SettingsChatImageQualityViewModel
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.SetChatImageQuality
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsChatImageQualityViewModelTest {

    private lateinit var underTest: SettingsChatImageQualityViewModel

    private val getChatImageQuality = mock<GetChatImageQuality> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val setChatImageQuality = mock<SetChatImageQuality>()

    private val scheduler = TestCoroutineScheduler()

    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = SettingsChatImageQualityViewModel(
            getChatImageQuality = getChatImageQuality,
            setChatImageQuality = setChatImageQuality,
            ioDispatcher = standardDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.selectedQuality).isNull()
            assertThat(initial.options).containsExactly(*ChatImageQuality.values())
        }
    }

    @Test
    fun `test that the option returned by getChatImageQuality is set as the selected option`() =
        runTest {
            whenever(getChatImageQuality()).thenReturn(flowOf(ChatImageQuality.Automatic))

            underTest.state.map { it.selectedQuality }.distinctUntilChanged().test {
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(ChatImageQuality.Automatic)
            }
        }

    @Test
    fun `test that selected quality is updated when a new value is emitted`() = runTest {
        whenever(getChatImageQuality()).thenReturn(
            flowOf(
                ChatImageQuality.Automatic,
                ChatImageQuality.Optimised
            )
        )

        underTest.state.map { it.selectedQuality }.distinctUntilChanged().test {
            assertThat(awaitItem()).isNull()
            assertThat(awaitItem()).isEqualTo(ChatImageQuality.Automatic)
            assertThat(awaitItem()).isEqualTo(ChatImageQuality.Optimised)
        }
    }

    @Test
    fun `test that setNewChatImageQuality calls the set use case with the correct value`() =
        runTest {
            underTest.setNewChatImageQuality(ChatImageQuality.Original)
            scheduler.advanceUntilIdle()
            verify(setChatImageQuality).invoke(ChatImageQuality.Original)
        }
}