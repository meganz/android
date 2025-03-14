package mega.privacy.android.app.presentation.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.chat.CreateNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.IsAnEmptyChatUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NoteToSelfChatViewModelTest {
    private lateinit var underTest: NoteToSelfChatViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase = mock()
    private val createNoteToSelfChatUseCase: CreateNoteToSelfChatUseCase = mock()
    private val isAnEmptyChatUseCase: IsAnEmptyChatUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        runBlocking {
            commonStub()
        }
        reset(
            getFeatureFlagValueUseCase,
            getNoteToSelfChatUseCase,
            createNoteToSelfChatUseCase,
            isAnEmptyChatUseCase
        )
    }

    private fun initTestClass() {
        runBlocking {
            commonStub()
        }
        underTest = NoteToSelfChatViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
            createNoteToSelfChatUseCase = createNoteToSelfChatUseCase,
            isAnEmptyChatUseCase = isAnEmptyChatUseCase,
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
        whenever(isAnEmptyChatUseCase(123L)).thenReturn(true)
        initTestClass()
        underTest.state.test {
            val updatedState = awaitItem()
            verifyNoInteractions(createNoteToSelfChatUseCase)
            Truth.assertThat(updatedState.noteToSelfChatId).isEqualTo(chatRoom.chatId)
            Truth.assertThat(updatedState.isNoteToSelfChatEmpty).isTrue()
        }
    }

    @Test
    fun `test that noteToSelfChatId updated when calling createNoteToSelfChatUseCase`() =
        runTest {
            val chatRoom = mock<ChatRoom> {
                on { chatId }.thenReturn(123L)
                on { isNoteToSelf }.thenReturn(true)
            }
            whenever(getNoteToSelfChatUseCase()).doReturn(null, chatRoom)
            whenever(isAnEmptyChatUseCase(123L)).thenReturn(true)
            whenever(createNoteToSelfChatUseCase()).thenReturn(123L)
            initTestClass()
            underTest.state.test {
                val updatedState = awaitItem()
                verify(createNoteToSelfChatUseCase).invoke()
                Truth.assertThat(updatedState.noteToSelfChatId).isEqualTo(chatRoom.chatId)
                Truth.assertThat(updatedState.isNoteToSelfChatEmpty).isTrue()
            }
        }
}