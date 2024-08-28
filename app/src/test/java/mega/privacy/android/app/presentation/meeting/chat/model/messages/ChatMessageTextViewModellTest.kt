package mega.privacy.android.app.presentation.meeting.chat.model.messages

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextViewModel
import mega.privacy.android.domain.usecase.chat.GetLinksFromMessageContentUseCase
import mega.privacy.android.domain.usecase.chat.link.EnableRichPreviewUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorRichLinkPreviewConfigUseCase
import mega.privacy.android.domain.usecase.chat.link.SetRichLinkWarningCounterUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageTextViewModellTest {

    private lateinit var underTest: ChatMessageTextViewModel

    private val monitorRichLinkPreviewConfigUseCase = mock<MonitorRichLinkPreviewConfigUseCase>()
    private val setRichLinkWarningCounterUseCase = mock<SetRichLinkWarningCounterUseCase>()
    private val enableRichPreviewUseCase = mock<EnableRichPreviewUseCase>()
    private val getLinksFromMessageContentUseCase = mock<GetLinksFromMessageContentUseCase>()

    @BeforeAll
    fun setup() {
        underTest = ChatMessageTextViewModel(
            monitorRichLinkPreviewConfigUseCase,
            setRichLinkWarningCounterUseCase,
            enableRichPreviewUseCase,
            getLinksFromMessageContentUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setRichLinkWarningCounterUseCase,
            enableRichPreviewUseCase,
            getLinksFromMessageContentUseCase,
        )
    }

    @Test
    fun `test that get links returns correctly`() = runTest {
        val link1 = "www.mega.io"
        val link2 = "www.google.es"
        val content = "Message with $link1 and $link2"
        val links = listOf(link1, link2)
        whenever(getLinksFromMessageContentUseCase(content)).thenReturn(links)
        Truth.assertThat(underTest.getLinks(content)).isEqualTo(links)
    }
}