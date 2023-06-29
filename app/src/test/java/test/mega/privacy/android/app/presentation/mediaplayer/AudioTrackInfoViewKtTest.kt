package test.mega.privacy.android.app.presentation.mediaplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.mediaplayer.trackinfo.AudioInfoView
import mega.privacy.android.app.mediaplayer.trackinfo.AudioNodeInfo
import mega.privacy.android.app.mediaplayer.trackinfo.AudioNodeInfoView
import mega.privacy.android.app.mediaplayer.trackinfo.AudioTrackInfoView
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ADDED_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ALBUM_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_ARTIST_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_DURATION_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_LAST_MODIFIED_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_LOCATION_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_SIZE_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.AUDIO_TITLE_TEST_TAG
import mega.privacy.android.app.mediaplayer.trackinfo.Constants.OFFLINE_OPTION_TEST_TAG
import mega.privacy.android.app.utils.LocationInfo
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import java.io.File

@RunWith(AndroidJUnit4::class)
internal class AudioTrackInfoViewKtTest {
    @get: Rule
    val composeTestRule = createComposeRule()

    private val expectTitleString = "Title test"
    private val expectAlbumString = "Album test"
    private val expectArtistString = "Artist test"
    private val expectedDuration = "06:24"
    private val expectedSizeValue = "128MB"
    private val expectedLocationValue = "Location test".uppercase()
    private val expectedLocationInfo = mock<LocationInfo> {
        on { location }.thenReturn(expectedLocationValue)
    }
    private val expectedAddedValue = "Added test"
    private val expectedLastModifiedValue = "Last modified test"
    private val metadataValue = mock<Metadata> {
        on { title }.thenReturn(expectTitleString)
        on { album }.thenReturn(expectAlbumString)
        on { artist }.thenReturn(expectArtistString)
    }
    private val metadata = Pair(metadataValue, expectedDuration)

    private val audioNodeInfo = mock<AudioNodeInfo> {
        on { availableOffline }.thenReturn(false)
        on { size }.thenReturn(expectedSizeValue)
        on { location }.thenReturn(expectedLocationInfo)
        on { added }.thenReturn(expectedAddedValue)
        on { lastModified }.thenReturn(expectedLastModifiedValue)
    }

    private fun setComposeContent(
        audioNodeInfo: AudioNodeInfo? = null,
        metadata: Pair<Metadata, String>? = null,
        onLocationClicked: (location: LocationInfo?) -> Unit = {},
        onCheckedChange: (isChecked: Boolean) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AudioTrackInfoView(
                audioNodeInfo = audioNodeInfo,
                metadata = metadata,
                onLocationClicked = onLocationClicked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    private fun setContentForAudioInfoView(
        thumbnail: File? = null,
        titleString: String? = null,
        artistString: String? = null,
        albumString: String? = null,
        duration: String? = null,
    ) {
        composeTestRule.setContent {
            AudioInfoView(
                thumbnail = thumbnail,
                titleString = titleString,
                artistString = artistString,
                albumString = albumString,
                duration = duration,
            )
        }
    }

    private fun setContentForAudioNodeInfoView(
        isEnabled: Boolean? = null,
        onCheckedChange: (isChecked: Boolean) -> Unit = {},
        sizeValue: String? = null,
        locationValue: LocationInfo? = null,
        onLocationClicked: (location: LocationInfo?) -> Unit = {},
        addedValue: String? = null,
        lastModifiedValue: String? = null,
    ) {
        composeTestRule.setContent {
            AudioNodeInfoView(
                onCheckedChange = onCheckedChange,
                isEnabled = isEnabled,
                sizeValue = sizeValue,
                locationValue = locationValue,
                onLocationClicked = onLocationClicked,
                addedValue = addedValue,
                lastModifiedValue = lastModifiedValue
            )
        }
    }

    @Test
    fun `test that artist and album are not displayed when artist and album are null`() {
        setContentForAudioInfoView()
        composeTestRule.onNodeWithTag(AUDIO_ARTIST_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AUDIO_ALBUM_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that views are displayed correctly when all values are not null`() {
        setComposeContent(audioNodeInfo = audioNodeInfo, metadata = metadata)

        composeTestRule.onNodeWithTag(AUDIO_TITLE_TEST_TAG).assertExists()
            .assertTextEquals(expectTitleString)
        composeTestRule.onNodeWithTag(AUDIO_ARTIST_TEST_TAG).assertExists()
            .assertTextEquals(expectArtistString)
        composeTestRule.onNodeWithTag(AUDIO_ALBUM_TEST_TAG).assertExists()
            .assertTextEquals(expectAlbumString)
        composeTestRule.onNodeWithTag(AUDIO_DURATION_TEST_TAG).assertExists()
            .assertTextEquals(expectedDuration)
        composeTestRule.onNodeWithTag(OFFLINE_OPTION_TEST_TAG).onChildren()
        composeTestRule.onNodeWithTag(OFFLINE_OPTION_TEST_TAG).assertIsToggleable()
        composeTestRule.onNodeWithTag(AUDIO_SIZE_TEST_TAG, true).assertExists()
            .assertTextEquals(expectedSizeValue)
        composeTestRule.onNodeWithTag(AUDIO_LOCATION_TEST_TAG, true).assertExists()
            .assertTextEquals(expectedLocationValue)
        composeTestRule.onNodeWithTag(AUDIO_ADDED_TEST_TAG, true).assertExists()
            .assertTextEquals(expectedAddedValue)
        composeTestRule.onNodeWithTag(AUDIO_LAST_MODIFIED_TEST_TAG, true).assertExists()
            .assertTextEquals(expectedLastModifiedValue)
    }

    @Test
    fun `test that views are displayed correctly when all values of audio info are not null`() {
        setContentForAudioInfoView(
            titleString = expectTitleString,
            artistString = expectArtistString,
            albumString = expectAlbumString,
            duration = expectedDuration
        )
        composeTestRule.onNodeWithTag(AUDIO_TITLE_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectTitleString)
        composeTestRule.onNodeWithTag(AUDIO_ARTIST_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectArtistString)
        composeTestRule.onNodeWithTag(AUDIO_ALBUM_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectAlbumString)
        composeTestRule.onNodeWithTag(AUDIO_DURATION_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectedDuration)
    }

    @Test
    fun `test that views are displayed correctly when all values of audio node info are not null`() {
        setContentForAudioNodeInfoView(
            sizeValue = expectedSizeValue,
            locationValue = expectedLocationInfo,
            addedValue = expectedAddedValue,
            lastModifiedValue = expectedLastModifiedValue
        )

        composeTestRule.onNodeWithTag(OFFLINE_OPTION_TEST_TAG).onChildren()
        composeTestRule.onNodeWithTag(OFFLINE_OPTION_TEST_TAG).assertIsToggleable()
        composeTestRule.onNodeWithTag(AUDIO_SIZE_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectedSizeValue)
        composeTestRule.onNodeWithTag(AUDIO_LOCATION_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectedLocationValue)
        composeTestRule.onNodeWithTag(AUDIO_ADDED_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectedAddedValue)
        composeTestRule.onNodeWithTag(AUDIO_LAST_MODIFIED_TEST_TAG).assertIsDisplayed()
            .assertTextEquals(expectedLastModifiedValue)
    }
}