package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAlbumRepositoryTest {
    private lateinit var underTest: AlbumRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val userSetMapper: UserSetMapper = ::createUserSet

    @Before
    fun setUp() {
        underTest = DefaultAlbumRepository(
            megaApiGateway = megaApiGateway,
            userSetMapper = userSetMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that createAlbum invokes the createSet api function`() = runTest {
        val testName = "Album 1"

        underTest.createAlbum(name = testName)

        verify(megaApiGateway).createSet(eq(testName), any())
    }

    @Test
    fun `test that addPhotosToAlbum invokes the createSetElement api function for each photoID`() =
        runTest {
            val testAlbumId = AlbumId(1L)
            val testPhoto = listOf(NodeId(1L), NodeId(2L))

            underTest.addPhotosToAlbum(albumID = testAlbumId, photosIDs = testPhoto)

            verify(megaApiGateway).createSetElement(testAlbumId.id, testPhoto[0].id)
            verify(megaApiGateway).createSetElement(testAlbumId.id, testPhoto[1].id)
        }

    @Test
    fun `test that addPhotosToAlbum does not invokes the createSetElement api function if the photos list is empty`() =
        runTest {
            val testAlbumId = AlbumId(1L)
            val testPhoto = emptyList<NodeId>()

            underTest.addPhotosToAlbum(albumID = testAlbumId, photosIDs = testPhoto)

            verifyNoInteractions(megaApiGateway)
        }

    @Test
    fun `test that getUserSets returns a single value if only one set exists`() = runTest {
        val expectedId = 1L
        val expectedName = "Album 1"

        val megaSet = mock<MegaSet> {
            on { id() }.thenReturn(expectedId)
            on { name() }.thenReturn(expectedName)
        }
        val megaSetList = mock<MegaSetList> {
            on { size() }.thenReturn(1L)
            on { get(any()) }.thenReturn(megaSet)
        }
        whenever(megaApiGateway.getSets()).thenReturn(megaSetList)

        val actualUserSets = underTest.getAllUserSets()

        assertThat(actualUserSets.size).isEqualTo(1)
        assertThat(actualUserSets[0].id).isEqualTo(expectedId)
        assertThat(actualUserSets[0].name).isEqualTo(expectedName)
    }

    @Test
    fun `test that getUserSets returns a the correct number of values if multiple sets exist`() =
        runTest {
            val expectedId = 1L
            val expectedName = "Album 1"
            val expectedSize = 5

            val megaSet = mock<MegaSet> {
                on { id() }.thenReturn(expectedId)
                on { name() }.thenReturn(expectedName)
            }
            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(expectedSize.toLong())
                on { get(any()) }.thenReturn(megaSet)
            }
            whenever(megaApiGateway.getSets()).thenReturn(megaSetList)

            val actualUserSets = underTest.getAllUserSets()

            assertThat(actualUserSets.size).isEqualTo(expectedSize)
        }


    @Test
    fun `getAlbumElementIDs should return correct result`() = runTest {
        val expectedNode = 1L
        val albumId = AlbumId(expectedNode)

        val megaSetElement = mock<MegaSetElement> {
            on { node() }.thenReturn(expectedNode)
        }
        val megaSetElementList = mock<MegaSetElementList> {
            on { size() }.thenReturn(1L)
            on { get(any()) }.thenReturn(megaSetElement)
        }
        whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)

        val actualElementIds = underTest.getAlbumElementIDs(albumId)

        assertThat(actualElementIds.size).isEqualTo(1)
        assertThat(actualElementIds[0].id).isEqualTo(expectedNode)
    }

    private fun createUserSet(
        id: Long,
        name: String,
        cover: Long?,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val cover: Long? = cover
    }
}
