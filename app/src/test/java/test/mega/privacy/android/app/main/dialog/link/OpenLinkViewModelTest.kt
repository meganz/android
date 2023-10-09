package test.mega.privacy.android.app.main.dialog.link

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment
import mega.privacy.android.app.main.dialog.link.OpenLinkViewModel
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpenLinkViewModelTest {
    private lateinit var underTest: OpenLinkViewModel
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase = mock()
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getUrlRegexPatternTypeUseCase,
            savedStateHandle,
            getHandleFromContactLinkUseCase,
            getChatLinkContentUseCase
        )
    }

    private fun initTestClass() {
        underTest = OpenLinkViewModel(
            getUrlRegexPatternTypeUseCase,
            savedStateHandle,
            getHandleFromContactLinkUseCase,
            getChatLinkContentUseCase
        )
    }

    @Test
    fun `test that uiState update correctly when call onLinkChanged`() = runTest {
        val link = "https://mega.nz/C!86YkxIDC"
        underTest.onLinkChanged(link)
        verify(savedStateHandle)[OpenLinkViewModel.CURRENT_INPUT_LINK] = link
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.linkType).isNull()
            Truth.assertThat(state.checkLinkResult).isNull()
            Truth.assertThat(state.submittedLink).isNull()
        }
    }

    @Test
    fun `test that inputLink returns correctly when call inputLink`() {
        val link = "https://mega.nz/C!86YkxIDC"
        whenever(savedStateHandle.get<String>(OpenLinkViewModel.CURRENT_INPUT_LINK)).thenReturn(link)
        Truth.assertThat(underTest.inputLink).isEqualTo(link)
    }

    @Test
    fun `test that uiState update correctly when call openLink with link`() = runTest {
        val link = "link"
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.submittedLink).isEqualTo(link)
        }
    }

    @Test
    fun `test that linkType update correctly when link is not chat link`() = runTest {
        val link = "https://mega.nz/C!86YkxIDC"
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(false)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(false)
        whenever(getUrlRegexPatternTypeUseCase(link)).thenReturn(RegexPatternType.CONTACT_LINK)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.linkType).isEqualTo(RegexPatternType.CONTACT_LINK)
        }
    }

    @Test
    fun `test that openContactLinkHandle update correctly when link is not chat link`() = runTest {
        val link = "https://mega.nz/C!86YkxIDC"
        whenever(getHandleFromContactLinkUseCase(link)).thenReturn(1L)
        underTest.openContactLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.openContactLinkHandle).isEqualTo(1L)
        }
    }

    @Test
    fun `test that checkLinkResult update correctly when open link to join meeting`() = runTest {
        val link = "https://mega.nz/C!86YkxIDC"
        val chatLinkContent = mock<ChatLinkContent.ChatLink>()
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(true)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(false)
        whenever(getChatLinkContentUseCase(link)).thenReturn(chatLinkContent)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.checkLinkResult).isEqualTo(Result.success(chatLinkContent))
            Truth.assertThat(state.linkType).isNull()
        }
    }

    @Test
    fun `test that checkLinkResult update correctly when open link from chat`() = runTest {
        val link = "https://mega.nz/C!86YkxIDC"
        val chatLinkContent = mock<ChatLinkContent.ChatLink>()
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_JOIN_MEETING))
            .thenReturn(false)
        whenever(savedStateHandle.get<Boolean>(OpenLinkDialogFragment.IS_CHAT_SCREEN))
            .thenReturn(true)
        whenever(getChatLinkContentUseCase(link)).thenReturn(chatLinkContent)
        initTestClass()
        underTest.openLink(link)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.checkLinkResult).isEqualTo(Result.success(chatLinkContent))
            Truth.assertThat(state.linkType).isNull()
        }
    }
}