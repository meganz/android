package mega.privacy.android.feature.photos.presentation.search

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.downloader.PhotoDownloaderViewModel
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Compose test class for [MediaSearchScreenM3]
 */
@Config(sdk = [33])
@RunWith(AndroidJUnit4::class)
class MediaSearchScreenM3ComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    private val mockPhotoDownloaderViewModel: PhotoDownloaderViewModel = mock()

    private fun setComposeContent(
        state: PhotosSearchState,
        onOpenAlbum: (AlbumContentNavKey) -> Unit = {},
        onOpenImagePreviewScreen: (Photo) -> Unit = {},
        onShowMoreMenu: (NodeId) -> Unit = {},
        onCloseScreen: () -> Unit = {},
        updateQuery: (String) -> Unit = {},
        updateSelectedQuery: (String?) -> Unit = {},
        updateRecentQueries: (String) -> Unit = {},
        searchPhotos: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MediaSearchScreenM3(
                state = state,
                photoDownloaderViewModel = mockPhotoDownloaderViewModel,
                onOpenAlbum = onOpenAlbum,
                onOpenImagePreviewScreen = onOpenImagePreviewScreen,
                onShowMoreMenu = onShowMoreMenu,
                onCloseScreen = onCloseScreen,
                updateQuery = updateQuery,
                updateSelectedQuery = updateSelectedQuery,
                updateRecentQueries = updateRecentQueries,
                searchPhotos = searchPhotos,
                modifier = Modifier,
            )
        }
    }

    @Test
    fun `test that welcome empty state is displayed when query is blank and no recent queries`() {
        val state = PhotosSearchState(
            contentState = MediaContentState.WelcomeEmpty,
            query = "",
            recentQueries = emptyList(),
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_WELCOME_EMPTY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that recent queries are displayed when query is blank and has recent queries`() {
        val recentQueries = listOf("vacation", "birthday", "family")
        val state = PhotosSearchState(
            contentState = MediaContentState.RecentQueries,
            query = "",
            recentQueries = recentQueries,
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_RECENT_QUERIES)
            .assertIsDisplayed()

        recentQueries.forEachIndexed { index, query ->
            composeTestRule
                .onNodeWithTag("$MEDIA_SEARCH_SCREEN_RECENT_QUERY_ITEM:$index")
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(query)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking recent query triggers callbacks`() {
        val recentQueries = listOf("vacation", "birthday")
        val state = PhotosSearchState(
            contentState = MediaContentState.RecentQueries,
            query = "",
            recentQueries = recentQueries,
        )
        val mockUpdateSelectedQuery: (String?) -> Unit = mock()
        val mockSearchPhotos: (String) -> Unit = mock()

        setComposeContent(
            state = state,
            updateSelectedQuery = mockUpdateSelectedQuery,
            searchPhotos = mockSearchPhotos,
        )

        composeTestRule
            .onNodeWithTag("$MEDIA_SEARCH_SCREEN_RECENT_QUERY_ITEM:0")
            .performClick()

        verify(mockUpdateSelectedQuery).invoke(recentQueries[0])
        verify(mockSearchPhotos).invoke(recentQueries[0])
    }

    @Test
    fun `test that no results empty state is displayed when search returns no results`() {
        val state = PhotosSearchState(
            contentState = MediaContentState.NoResults,
            query = "nonexistent",
            photos = emptyList(),
            legacyAlbums = emptyList(),
            isSearchingPhotos = false,
            isSearchingAlbums = false,
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_NO_RESULTS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that search results are displayed when photos exist`() {
        val photos = listOf(
            createMockPhoto(id = 1L, name = "Photo 1"),
            createMockPhoto(id = 2L, name = "Photo 2"),
        )
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "photo",
            photos = photos,
            legacyAlbums = emptyList(),
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_RESULTS)
            .assertIsDisplayed()

        photos.forEachIndexed { index, _ ->
            composeTestRule
                .onNodeWithTag("$MEDIA_SEARCH_SCREEN_PHOTO_ITEM:$index")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that search results are displayed when albums exist`() {
        val albums = listOf(
            createMockUIAlbum(id = 1L, title = "Album 1"),
            createMockUIAlbum(id = 2L, title = "Album 2"),
        )
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "album",
            photos = emptyList(),
            albums = albums,
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_RESULTS)
            .assertIsDisplayed()

        albums.forEachIndexed { index, _ ->
            composeTestRule
                .onNodeWithTag("$MEDIA_SEARCH_SCREEN_ALBUM_ITEM:$index")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking photo triggers callback`() {
        val photos = listOf(createMockPhoto(id = 1L, name = "Photo 1"))
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "photo",
            photos = photos,
            legacyAlbums = emptyList(),
        )
        val mockOnOpenImagePreviewScreen: (Photo) -> Unit = mock()

        setComposeContent(
            state = state,
            onOpenImagePreviewScreen = mockOnOpenImagePreviewScreen,
        )

        composeTestRule
            .onNodeWithTag("$MEDIA_SEARCH_SCREEN_PHOTO_ITEM:0")
            .performClick()

        verify(mockOnOpenImagePreviewScreen).invoke(any())
    }

    @Test
    fun `test that clicking album triggers callback`() {
        val albums = listOf(createMockUIAlbum(id = 1L, title = "Album 1"))
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "album",
            photos = emptyList(),
            albums = albums,
        )
        val mockOnOpenAlbum: (AlbumContentNavKey) -> Unit = mock()

        setComposeContent(
            state = state,
            onOpenAlbum = mockOnOpenAlbum,
        )

        composeTestRule
            .onNodeWithTag("$MEDIA_SEARCH_SCREEN_ALBUM_ITEM:0")
            .performClick()

        verify(mockOnOpenAlbum).invoke(any())
    }

    @Test
    fun `test that nothing is displayed when initializing`() {
        val state = PhotosSearchState(
            contentState = MediaContentState.Initializing,
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_WELCOME_EMPTY)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_RECENT_QUERIES)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_NO_RESULTS)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(MEDIA_SEARCH_SCREEN_RESULTS)
            .assertDoesNotExist()
    }

    @Test
    fun `test that recent search header is displayed when recent queries exist`() {
        val state = PhotosSearchState(
            contentState = MediaContentState.RecentQueries,
            query = "",
            recentQueries = listOf("test query"),
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.photos_search_recent_search))
            .assertIsDisplayed()
    }

    @Test
    fun `test that albums header is displayed when albums exist in results`() {
        val albums = listOf(createMockUIAlbum(id = 1L, title = "Test Album"))
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "test",
            albums = albums,
            photos = emptyList(),
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.media_albums_tab_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that photos header is displayed when photos exist in results`() {
        val photos = listOf(createMockPhoto(id = 1L, name = "Test Photo"))
        val state = PhotosSearchState(
            contentState = MediaContentState.SearchResults,
            query = "test",
            photos = photos,
            legacyAlbums = emptyList(),
        )

        setComposeContent(state)

        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.media_feature_title))
            .assertIsDisplayed()
    }

    private fun createMockPhoto(
        id: Long,
        name: String = "test.jpg",
        isFavourite: Boolean = false,
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Image = Photo.Image(
        id = id,
        albumPhotoId = null,
        parentId = 0L,
        name = name,
        isFavourite = isFavourite,
        creationTime = LocalDateTime.now(),
        modificationTime = LocalDateTime.now(),
        thumbnailFilePath = null,
        previewFilePath = null,
        fileTypeInfo = mock<StaticImageFileTypeInfo>(),
        size = 0L,
        isTakenDown = false,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
        base64Id = null,
    )

    private fun createMockUIAlbum(
        id: Long,
        title: String,
    ): AlbumUiState {
        val album = MediaAlbum.User(
            id = AlbumId(id),
            title = title,
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )
        return AlbumUiState(
            mediaAlbum = album,
            title = title,
            isExported = false,
            cover = null,
        )
    }
}
