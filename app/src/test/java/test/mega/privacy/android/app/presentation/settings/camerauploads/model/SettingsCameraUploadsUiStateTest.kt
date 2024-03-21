package test.mega.privacy.android.app.presentation.settings.camerauploads.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [SettingsCameraUploadsUiState]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsCameraUploadsUiStateTest {

    private lateinit var underTest: SettingsCameraUploadsUiState

    @ParameterizedTest(name = "when upload option ui item is {0}, then can change location tags state is {1}")
    @MethodSource("provideChangeLocationTagsStateParameters")
    fun `test that the user could be allowed to change the location tags state`(
        uploadOptionUiItem: UploadOptionUiItem,
        canChangeLocationTagsState: Boolean,
    ) {
        underTest = SettingsCameraUploadsUiState(uploadOptionUiItem = uploadOptionUiItem)

        assertThat(underTest.canChangeLocationTagsState).isEqualTo(canChangeLocationTagsState)
    }

    private fun provideChangeLocationTagsStateParameters() = Stream.of(
        Arguments.of(UploadOptionUiItem.PhotosOnly, true),
        Arguments.of(UploadOptionUiItem.VideosOnly, false),
        Arguments.of(UploadOptionUiItem.PhotosAndVideos, true),
    )

    @ParameterizedTest(name = "when upload option ui item is {0}, then can change video quality is {1}")
    @MethodSource("provideChangeVideoQualityParameters")
    fun `test that the user could be allowed to change the video quality`(
        uploadOptionUiItem: UploadOptionUiItem,
        canChangeVideoQuality: Boolean,
    ) {
        underTest = SettingsCameraUploadsUiState(uploadOptionUiItem = uploadOptionUiItem)

        assertThat(underTest.canChangeVideoQuality).isEqualTo(canChangeVideoQuality)
    }

    private fun provideChangeVideoQualityParameters() = Stream.of(
        Arguments.of(UploadOptionUiItem.PhotosOnly, false),
        Arguments.of(UploadOptionUiItem.VideosOnly, true),
        Arguments.of(UploadOptionUiItem.PhotosAndVideos, true),
    )
}