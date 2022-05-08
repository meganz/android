package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.domain.usecase.GetCameraUploadFolder
import mega.privacy.android.app.domain.usecase.GetMediaUploadFolder
import mega.privacy.android.app.domain.usecase.GetThumbnail
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getAllFavorites = mock<GetAllFavorites>()
    private val cameraUploadFolder = mock<GetCameraUploadFolder>()
    private val mediaUploadFolder = mock<GetMediaUploadFolder>()
    private val getThumbnail = mock<GetThumbnail>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = AlbumsViewModel(
            getAllFavorites = getAllFavorites,
            cameraUploadFolder = cameraUploadFolder,
            mediaUploadFolder = mediaUploadFolder,
            getThumbnail = getThumbnail
        )
    }

    @Test
    fun `test default state`() = runTest {
        underTest.favouritesState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and there is no favourite item`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flowOf(emptyList())
        )
    }

    @Test
    fun `test that start with loading state and load favourites success`() = runTest {
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
        whenever(getAllFavorites()).thenReturn(
            flowOf(list)
        )
    }
}