package test.mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FavouriteFolderViewModelTest {
    private lateinit var underTest: FavouriteFolderViewModel

    private val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouriteFolderViewModel(
            context = mock(),
            getFavouriteFolderInfo = getFavouriteFolderInfo,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = mock(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `test default is loading state` () = runTest {
        underTest.childrenNodesState.test {
            assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
        }
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
        val node = mock<MegaNode>()
        whenever(node.handle).thenReturn(123)
        whenever(node.parentHandle).thenReturn(1234)
        whenever(node.base64Handle).thenReturn("base64Handle")
        whenever(node.modificationTime).thenReturn(1234567890)
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(node.name).thenReturn("testName.txt")
        val favourite = FavouriteInfo(
            id = node.handle,
            parentId = node.parentHandle,
            base64Id = node.base64Handle,
            modificationTime = node.modificationTime,
            node = node,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0
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
        underTest.childrenNodesState.test {
            assertTrue(awaitItem() is ChildrenNodesLoadState.Loading)
            assertTrue(awaitItem() is ChildrenNodesLoadState.Success)
        }
    }
}