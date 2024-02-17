package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.usecase.GetAllGalleryImages
import mega.privacy.android.domain.usecase.GetAllGalleryVideos
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatGalleryViewModelTest {
    private lateinit var underTest: ChatGalleryViewModel
    private val getAllGalleryImages: GetAllGalleryImages = mock()
    private val getAllGalleryVideos: GetAllGalleryVideos = mock()

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getAllGalleryImages, getAllGalleryVideos)
    }

    private fun initTestClass() {
        underTest = ChatGalleryViewModel(getAllGalleryImages, getAllGalleryVideos)
    }

    @Test
    fun `test that loadGalleryImages update state correctly`() = runTest {
        val image = mock<FileGalleryItem> {
            on { dateAdded } doReturn 1
        }
        val video = mock<FileGalleryItem> {
            on { dateAdded } doReturn 2
        }
        whenever(getAllGalleryImages()).thenReturn(flowOf(image))
        whenever(getAllGalleryVideos()).thenReturn(flowOf(video))
        underTest.loadGalleryImages()
        underTest.state.test {
            val item = awaitItem()
            assertThat(item.isLoading).isFalse()
            assertThat(item.items).containsExactly(video, image)
        }
    }
}