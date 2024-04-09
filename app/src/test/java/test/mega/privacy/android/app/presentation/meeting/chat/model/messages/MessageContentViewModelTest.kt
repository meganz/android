package test.mega.privacy.android.app.presentation.meeting.chat.model.messages

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.model.messages.MessageContentViewModel
import mega.privacy.android.domain.usecase.chat.GetLinksFromMessageContentUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageContentViewModelTest {

    private lateinit var underTest: MessageContentViewModel

    private val getLinksFromMessageContentUseCase = mock<GetLinksFromMessageContentUseCase>()

    @BeforeAll
    fun setup() {
        underTest = MessageContentViewModel(getLinksFromMessageContentUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getLinksFromMessageContentUseCase)
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