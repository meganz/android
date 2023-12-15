package test.mega.privacy.android.app.presentation.settings.chat.imagequality

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.chat.imagequality.SettingsChatImageQualityViewModel
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.SetChatImageQuality
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsChatImageQualityViewModelTest {

    private lateinit var underTest: SettingsChatImageQualityViewModel

    private var getQualityFlow = MutableSharedFlow<ChatImageQuality>()
    private val getChatImageQuality = mock<GetChatImageQuality>()
    private val setChatImageQuality = mock<SetChatImageQuality>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    private fun initTestClass() {
        underTest = SettingsChatImageQualityViewModel(
            getChatImageQuality = getChatImageQuality,
            setChatImageQuality = setChatImageQuality,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(setChatImageQuality)
        wheneverBlocking { getChatImageQuality() }.thenReturn(getQualityFlow)
        initTestClass()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.selectedQuality).isNull()
            assertThat(initial.options).containsExactly(*ChatImageQuality.entries.toTypedArray())
        }
    }

    @Test
    fun `test that the option returned by getChatImageQuality is set as the selected option`() =
        runTest {
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.selectedQuality }.drop(1).test {
                getQualityFlow.emit(ChatImageQuality.Original)
                assertThat(awaitItem()).isEqualTo(ChatImageQuality.Original)
            }
        }

    @Test
    fun `test that selected quality is updated when a new value is emitted`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.selectedQuality }.drop(1).test {
            getQualityFlow.emit(ChatImageQuality.Original)
            assertThat(awaitItem()).isEqualTo(ChatImageQuality.Original)
            getQualityFlow.emit(ChatImageQuality.Optimised)
            assertThat(awaitItem()).isEqualTo(ChatImageQuality.Optimised)
        }
    }

    @Test
    fun `test that setNewChatImageQuality calls the set use case with the correct value`() =
        runTest {
            whenever(setChatImageQuality(ChatImageQuality.Original)).thenReturn(Unit)
            underTest.setNewChatImageQuality(ChatImageQuality.Original)
            testScheduler.advanceUntilIdle()
            verify(setChatImageQuality).invoke(ChatImageQuality.Original)
        }
}