package mega.privacy.android.app.presentation.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatNewLabelPreferenceUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import mega.privacy.android.domain.usecase.chat.SetNoteToSelfChatNewLabelPreferenceUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NoteToSelfChatViewModelTest {
    private lateinit var underTest: NoteToSelfChatViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase = mock()
    private val monitorNoteToSelfChatIsEmptyUseCase: MonitorNoteToSelfChatIsEmptyUseCase = mock()
    private val getNoteToSelfChatPreferenceUseCase: GetNoteToSelfChatNewLabelPreferenceUseCase =
        mock()
    private val setNoteToSelfChatPreferenceUseCase: SetNoteToSelfChatNewLabelPreferenceUseCase =
        mock()

    @BeforeEach
    fun resetMocks() {
        runBlocking {
            commonStub()
        }
        reset(
            getFeatureFlagValueUseCase,
            getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase,
            getNoteToSelfChatPreferenceUseCase,
            setNoteToSelfChatPreferenceUseCase
        )
    }

    private fun initTestClass() {
        runBlocking {
            commonStub()
        }
        underTest = NoteToSelfChatViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase = monitorNoteToSelfChatIsEmptyUseCase,
            getNoteToSelfChatPreferenceUseCase = getNoteToSelfChatPreferenceUseCase,
            setNoteToSelfChatPreferenceUseCase = setNoteToSelfChatPreferenceUseCase
        )
    }

    private fun commonStub() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.NoteToYourselfFlag)).thenReturn(true)
    }

    @Test
    fun `test that noteToSelfChatId updated when calling getNoteToSelfChatUseCase`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { chatId }.thenReturn(123L)
            on { isNoteToSelf }.thenReturn(true)
        }
        whenever(getNoteToSelfChatUseCase()).thenReturn(chatRoom)
        whenever(monitorNoteToSelfChatIsEmptyUseCase(123L)).thenReturn(
            flowOf(true)
        )
        initTestClass()
        underTest.state.test {
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.noteToSelfChatId).isEqualTo(chatRoom.chatId)
            Truth.assertThat(updatedState.isNoteToSelfChatEmpty).isTrue()
        }
    }

    @Test
    fun `test that newFeatureLabelCounter updated when calling getNoteToSelfPreference and it is established`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(123L)
                on { isNoteToSelf }.thenReturn(true)
            }
            val invalidValue = -1
            val currentValue = 3
            val newValue = 2

            whenever(getNoteToSelfChatUseCase()).thenReturn(chatRoom)
            whenever(monitorNoteToSelfChatIsEmptyUseCase(123L)).thenReturn(
                flowOf(true)
            )
            whenever(getNoteToSelfChatPreferenceUseCase()).thenReturn(currentValue)
            initTestClass()

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.newFeatureLabelCounter).isEqualTo(invalidValue)
                underTest.getNoteToSelfPreference()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.newFeatureLabelCounter).isEqualTo(newValue)
            }
        }


    @Test
    fun `test that newFeatureLabelCounter updated when calling getNoteToSelfPreference and it is not established`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(123L)
                on { isNoteToSelf }.thenReturn(true)
            }
            val maxValue = 5
            val invalidValue = -1
            whenever(getNoteToSelfChatUseCase()).thenReturn(chatRoom)
            whenever(monitorNoteToSelfChatIsEmptyUseCase(123L)).thenReturn(
                flowOf(true)
            )
            whenever(getNoteToSelfChatPreferenceUseCase()).thenReturn(invalidValue)
            initTestClass()

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.newFeatureLabelCounter).isEqualTo(invalidValue)
                underTest.getNoteToSelfPreference()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.newFeatureLabelCounter).isEqualTo(maxValue)
            }
        }

    @Test
    fun `test that newFeatureLabelCounter updated when calling getNoteToSelfPreference and it's consumed`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(123L)
                on { isNoteToSelf }.thenReturn(true)
            }
            val invalidValue = -1
            val consumedValue = 0
            whenever(getNoteToSelfChatUseCase()).thenReturn(chatRoom)
            whenever(monitorNoteToSelfChatIsEmptyUseCase(123L)).thenReturn(
                flowOf(true)
            )
            whenever(getNoteToSelfChatPreferenceUseCase()).thenReturn(consumedValue)
            initTestClass()

            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.newFeatureLabelCounter).isEqualTo(invalidValue)
                underTest.getNoteToSelfPreference()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.newFeatureLabelCounter).isEqualTo(consumedValue)
            }
        }
}