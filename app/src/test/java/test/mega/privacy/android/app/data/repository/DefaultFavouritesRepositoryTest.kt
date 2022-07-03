package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.repository.DefaultFavouritesRepository
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.repository.FavouritesRepository
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
    private val favouriteInfoMapper = mock<FavouriteInfoMapper>()

    private val node = mock<MegaNode> {
        on { handle }.thenReturn(1L)
        on { name }.thenReturn("testName")
        on { size }.thenReturn(1000L)
        on { label }.thenReturn(MegaNode.NODE_LBL_RED)
    }

    private val gateway = mock<MegaApiGateway> {
        on { hasVersion(node) }.thenReturn(false)
        on { getNumChildFolders(node) }.thenReturn(0)
        on { getNumChildFiles(node) }.thenReturn(0)
    }

    private val favouriteInfo = FavouriteInfo(
        id = 0,
        name = node.name,
        size = node.size,
        label = node.label,
        parentId = 0,
        base64Id = "",
        modificationTime = 0L,
        hasVersion = true,
        numChildFiles = 0,
        numChildFolders = 0,
        isImage = false,
        isVideo = false,
        isFolder = true,
        isFavourite = true,
        isExported = false,
        isTakenDown = false,
    )

    @Before
    fun setUp() {
        underTest = DefaultFavouritesRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            monitorNodeChangeFacade = MonitorNodeChangeFacade(),
            favouriteInfoMapper = favouriteInfoMapper,
            cacheFolder = mock()
        )
    }

    @Test
    fun `test that get all favourites returns successfully if no error is thrown`() {
        runTest {
            val megaHandleListTest = mock<MegaHandleList>()
            whenever(megaHandleListTest.size()).thenReturn(1)
            whenever(megaHandleListTest[0]).thenReturn(1L)

            val gateway = mock<MegaApiGateway> {
                on { hasVersion(node) }.thenReturn(false)
                on { getNumChildFolders(node) }.thenReturn(0)
                on { getNumChildFiles(node) }.thenReturn(0)
            }

            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(node)
            whenever(favouriteInfoMapper(node,
                null,
                gateway.hasVersion(node),
                gateway.getNumChildFolders(node),
                gateway.getNumChildFiles(node))).thenReturn(
                favouriteInfo
            )

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
    }

    @Test(expected = MegaException::class)
    fun `test that get all favourites returns doesn't successfully`() {
        runTest {
            val megaHandleListTest = mock<MegaHandleList>()
            whenever(megaHandleListTest.size()).thenReturn(1)
            whenever(megaHandleListTest[0]).thenReturn(1L)

            val gateway = mock<MegaApiGateway> {
                on { hasVersion(node) }.thenReturn(false)
                on { getNumChildFolders(node) }.thenReturn(0)
                on { getNumChildFiles(node) }.thenReturn(0)
            }

            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(node)
            whenever(favouriteInfoMapper(node,
                null,
                gateway.hasVersion(node),
                gateway.getNumChildFolders(node),
                gateway.getNumChildFiles(node))).thenReturn(
                favouriteInfo
            )

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
    }

    @Test
    fun `test that get children returns successfully if no error is thrown`() {
        runTest {
            val expectedParentHandle = 0L
            val expectedParentName = "parentNodeName"
            val expectedParentNodeHandle = 1L
            val parentNode = mock<MegaNode> {
                on { name }.thenReturn(expectedParentName)
                on { parentHandle }.thenReturn(expectedParentNodeHandle)
            }
            val favouriteInfo = FavouriteInfo(
                id = 0,
                name = node.name,
                size = node.size,
                label = node.label,
                parentId = 0,
                base64Id = "",
                modificationTime = 0L,
                hasVersion = true,
                numChildFiles = 0,
                numChildFolders = 0,
                isImage = false,
                isVideo = false,
                isFolder = true,
                isFavourite = true,
                isExported = false,
                isTakenDown = false,
            )

            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(parentNode)
            whenever(megaApiGateway.getChildrenByNode(parentNode)).thenReturn(arrayListOf(node))

            whenever(favouriteInfoMapper(node,
                null,
                gateway.hasVersion(node),
                gateway.getNumChildFolders(node),
                gateway.getNumChildFiles(node))).thenReturn(
                favouriteInfo
            )

            val actual = underTest.getChildren(expectedParentHandle)

            val favouriteFolderInfo = FavouriteFolderInfo(
                children = listOf(favouriteInfo),
                name = parentNode.name,
                currentHandle = expectedParentHandle,
                parentHandle = parentNode.parentHandle
            )

            assertThat(actual?.children?.get(0)).isSameInstanceAs(favouriteFolderInfo.children[0])
            assertThat(actual?.currentHandle).isEqualTo(favouriteFolderInfo.currentHandle)
            assertThat(actual?.parentHandle).isEqualTo(favouriteFolderInfo.parentHandle)
            assertThat(actual?.name).isEqualTo(favouriteFolderInfo.name)
        }
    }
}