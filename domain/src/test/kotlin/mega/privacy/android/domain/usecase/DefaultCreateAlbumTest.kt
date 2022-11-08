package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class DefaultCreateAlbumTest {
    private lateinit var underTest: CreateAlbum
    private val photosRepository = mock<PhotosRepository>()
    private val albumRepository = mock<AlbumRepository>()

    private val testName = "Album1"

    @Before
    fun setUp() {
        underTest = DefaultCreateAlbum(
            photosRepository = photosRepository,
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that the UserAlbum returned has the same name as the given argument`() = runTest {
        val expectedUserSet = createUserSet(name = testName)

        whenever(albumRepository.createAlbum(any())).thenReturn(
            expectedUserSet
        )

        val actualNewAlbum = underTest(testName)

        assertEquals(testName, actualNewAlbum.title)
    }

    private fun createUserSet(
        id: Long = 1L,
        name: String = "NewAlbum",
        cover: Long = 10L,
        modificationTime: Long = 2L,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val cover: Long = cover

        override val modificationTime: Long = modificationTime
    }
}