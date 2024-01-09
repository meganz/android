package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class CreateAlbumUseCaseTest {
    private lateinit var underTest: CreateAlbumUseCase
    private val albumRepository = mock<AlbumRepository>()

    private val testName = "Album1"

    @Before
    fun setUp() {
        underTest = CreateAlbumUseCase(
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
        type: Int = 0,
        cover: Long = 10L,
        creationTime: Long = 2L,
        modificationTime: Long = 2L,
        isExported: Boolean = false,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val type: Int = type

        override val cover: Long = cover

        override val creationTime: Long = creationTime

        override val modificationTime: Long = modificationTime

        override val isExported: Boolean = isExported
    }
}