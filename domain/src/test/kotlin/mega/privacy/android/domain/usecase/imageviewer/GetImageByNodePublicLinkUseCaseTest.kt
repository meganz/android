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
class GetImageByNodePublicLinkUseCaseTest {
    private lateinit var underTest: GetImageByNodePublicLinkUseCase

    private val networkRepository = mock<NetworkRepository>()
    private val imageRepository = mock<ImageRepository>()

    @Before
    fun setUp() {
        underTest = GetImageByNodePublicLinkUseCase(networkRepository, imageRepository)
    }

    @Test
    fun `test that network repository function is called on invoke`() {
        runTest {
            underTest.invoke("", fullSize = false, highPriority = false, resetDownloads = {})
            verify(networkRepository, times(1)).isMeteredConnection()
        }
    }

    @Test
    fun `test that image repository function is called on invoke`() {
        runTest {
            val nodeFileLink = "abc"
            val fullSize = false
            val highPriority = true
            val isMeteredConnection = false
            val resetDownloads = {}

            underTest.invoke(
                nodeFileLink = nodeFileLink,
                fullSize = fullSize,
                highPriority = highPriority,
                resetDownloads = resetDownloads
            )

            whenever(networkRepository.isMeteredConnection()).thenReturn(isMeteredConnection)

            verify(imageRepository, times(1)).getImageByNodePublicLink(
                nodeFileLink = nodeFileLink,
                fullSize = fullSize,
                highPriority = highPriority,
                isMeteredConnection = isMeteredConnection,
                resetDownloads = resetDownloads
            )
        }
    }
}