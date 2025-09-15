package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.FavouritesRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFavouritesRepositoryTest {
    private lateinit var underTest: FavouritesRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val node = mock<MegaNode> {
        on { handle }.thenReturn(1L)
        on { name }.thenReturn("testName")
        on { size }.thenReturn(1000L)
        on { label }.thenReturn(MegaNode.NODE_LBL_RED)
    }

    private val favouriteInfo = mock<FolderNode>()

    private val nodeMapper: NodeMapper = mock()

    @Before
    fun setUp() {
        underTest = DefaultFavouritesRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            nodeMapper = nodeMapper,
        )
    }

    @Test
    fun `test that get all favourites returns successfully if no error is thrown`() = runTest {
        val megaHandleListTest = mock<MegaHandleList>()
        whenever(megaHandleListTest.size()).thenReturn(1)
        whenever(megaHandleListTest[0]).thenReturn(1L)

        whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(node)

        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { megaHandleList }.thenReturn(megaHandleListTest)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.getFavourites(anyOrNull(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        whenever(
            nodeMapper(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            favouriteInfo
        )

        val actual = underTest.getAllFavorites()
        assertThat(actual[0]).isSameInstanceAs(favouriteInfo)
    }


    @Test(expected = MegaException::class)
    fun `test that get an exception is thrown if the api does not return successfully`() = runTest {
        val megaHandleListTest = mock<MegaHandleList>()
        whenever(megaHandleListTest.size()).thenReturn(1)
        whenever(megaHandleListTest[0]).thenReturn(1L)

        whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(node)

        val api = mock<MegaApiJava>()
        val request = mock<MegaRequest> {
            on { megaHandleList }.thenReturn(megaHandleListTest)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK + 1)
        }

        whenever(megaApiGateway.getFavourites(anyOrNull(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }

        whenever(
            nodeMapper(
                any(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            favouriteInfo
        )

        underTest.getAllFavorites()
    }

    @Test
    fun `test that add favourites works properly`() = runTest {
        val nodeIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )

        whenever(megaApiGateway.getMegaNodeByHandle(any()))
            .thenReturn(mock())
        underTest.addFavourites(nodeIds)
        verify(megaApiGateway, times(3)).setNodeFavourite(any(), any())
    }
}