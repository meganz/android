package test.mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.FavouriteInfo
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FavouriteFolderViewModelTest {
    private lateinit var underTest: FavouriteFolderViewModel

    private val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()
    private val favouriteMapper = mock<FavouriteMapper>()

    private val megaNode = mock<MegaNode>()

    private val fetchNodeWrapper = mock<FetchNodeWrapper> {
        onBlocking { invoke(any()) }.thenReturn(
            megaNode)
    }

    private val megaUtilWrapper = mock<MegaUtilWrapper>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouriteFolderViewModel(
            context = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getFavouriteFolderInfo = getFavouriteFolderInfo,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = megaUtilWrapper,
            savedStateHandle = SavedStateHandle(),
            favouriteMapper = favouriteMapper,
            fetchNode = fetchNodeWrapper,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that start with loading state and children nodes is empty`() = runTest {
        whenever(getFavouriteFolderInfo(-1)).thenReturn(
            flowOf(
                FavouriteFolderInfo(
                    emptyList(),
                    "test name",
                    1,
                    1
                )
            )
        )
        underTest.childrenNodesState.test {
            assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
            assertTrue(awaitItem() is ChildrenNodesLoadState.Empty)
        }
    }

    @Test
    fun `test that start with loading state and children nodes is not empty`() = runTest {
        val node = megaNode
        whenever(node.handle).thenReturn(123)
        whenever(node.label).thenReturn(MegaNode.NODE_LBL_RED)
        whenever(node.size).thenReturn(1000L)
        whenever(node.parentHandle).thenReturn(1234)
        whenever(node.base64Handle).thenReturn("base64Handle")
        whenever(node.modificationTime).thenReturn(1234567890)
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(node.name).thenReturn("testName.txt")
        val favourite = FavouriteInfo(
            id = node.handle,
            name = node.name,
            label = node.label,
            size = node.size,
            parentId = node.parentHandle,
            base64Id = node.base64Handle,
            modificationTime = node.modificationTime,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0,
            isImage = false,
            isVideo = false,
            isFolder = true,
            isFavourite = true,
            isExported = false,
            isTakenDown = false,
        )
        val list = listOf(favourite)
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        whenever(getFavouriteFolderInfo(-1)).thenReturn(
            flowOf(
                FavouriteFolderInfo(
                    list,
                    "testName",
                    1,
                    1
                )
            )
        )
        whenever(favouriteMapper(any(), any(), any(), any(), any())).thenReturn(mock())
        whenever(fetchNodeWrapper(anyOrNull())).thenReturn(node)
        whenever(megaUtilWrapper.availableOffline(anyOrNull(),
            anyOrNull())).thenReturn(true)
        underTest.childrenNodesState.test {
            assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
            assertTrue(awaitItem() is ChildrenNodesLoadState.Success)
        }
    }
}