package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DefaultCreateAlbumTest {

    lateinit var underTest: CreateAlbum
    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DefaultCreateAlbum(albumRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that the repository createAlbum is called`() = runTest {
        val testName = "Album 1"

        underTest(testName)

        verify(albumRepository).createAlbum(testName)
    }


}