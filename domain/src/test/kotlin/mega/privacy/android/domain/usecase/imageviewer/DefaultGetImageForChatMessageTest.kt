package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetImageForChatMessageTest {
    private lateinit var underTest: GetImageForChatMessage

    private val networkRepository = mock<NetworkRepository>()
    private val imageRepository = mock<ImageRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetImageForChatMessage(networkRepository, imageRepository)
    }

    @Test
    fun `test that network repository function is called on invoke`() {
        runTest {
            underTest.invoke(
                chatRoomId = 1L,
                chatMessageId = 1L,
                fullSize = false,
                highPriority = false,
                resetDownloads = {}
            )
            verify(networkRepository, times(1)).isMeteredConnection()
        }
    }

    @Test
    fun `test that image repository function is called on invoke`() {
        runTest {
            val chatRoomId = 1L
            val chatMessageId = 1L
            val fullSize = false
            val highPriority = true
            val isMeteredConnection = false
            val resetDownloads = {}

            underTest.invoke(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = fullSize,
                highPriority = highPriority,
                resetDownloads = resetDownloads
            )

            whenever(networkRepository.isMeteredConnection()).thenReturn(isMeteredConnection)

            verify(imageRepository, times(1)).getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = fullSize,
                highPriority = highPriority,
                isMeteredConnection = isMeteredConnection,
                resetDownloads = resetDownloads
            )
        }
    }
}