package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.repository.DefaultFavouritesRepository
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FavouriteFolderInfoMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.Node
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFavouritesRepositoryTest {
    private lateinit var underTest: FavouritesRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>()

    private val node = mock<MegaNode> {
        on { handle }.thenReturn(1L)
        on { name }.thenReturn("testName")
        on { size }.thenReturn(1000L)
        on { label }.thenReturn(MegaNode.NODE_LBL_RED)
    }

    private val favouriteInfo = mock<Node>()

    private val nodeMapper: NodeMapper = { _, _, _, _, _, _, _, _ -> favouriteInfo }

    private val favouriteFolderInfoMapper = mock<FavouriteFolderInfoMapper>()


    @Before
    fun setUp() {
        underTest = DefaultFavouritesRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            monitorNodeChangeFacade = MonitorNodeChangeFacade(),
            nodeMapper = nodeMapper,
            favouriteFolderInfoMapper = favouriteFolderInfoMapper,
            cacheFolder = mock(),
            fileTypeInfoMapper = fileTypeInfoMapper
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

        underTest.getAllFavorites()
    }


    @Test
    fun `test that get children returns successfully if no error is thrown`() = runTest {
        val expectedParentHandle = 0L
        val expectedParentName = "parentNodeName"
        val expectedParentNodeHandle = 1L
        val parentNode = mock<MegaNode> {
            on { name }.thenReturn(expectedParentName)
            on { parentHandle }.thenReturn(expectedParentNodeHandle)
        }
        val favouriteFolderInfo = FavouriteFolderInfo(
            children = listOf(favouriteInfo),
            name = parentNode.name,
            currentHandle = expectedParentHandle,
            parentHandle = parentNode.parentHandle
        )

        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(parentNode)
        whenever(megaApiGateway.getChildrenByNode(parentNode)).thenReturn(arrayListOf(node))
        whenever(
            favouriteFolderInfoMapper(
                parentNode,
                listOf(favouriteInfo),
                expectedParentHandle,
            )
        ).thenReturn(favouriteFolderInfo)

        val actual = underTest.getChildren(expectedParentHandle)

        assertThat(actual?.children?.get(0)).isSameInstanceAs(favouriteFolderInfo.children[0])
        assertThat(actual?.currentHandle).isEqualTo(favouriteFolderInfo.currentHandle)
        assertThat(actual?.parentHandle).isEqualTo(favouriteFolderInfo.parentHandle)
        assertThat(actual?.name).isEqualTo(favouriteFolderInfo.name)
    }

}