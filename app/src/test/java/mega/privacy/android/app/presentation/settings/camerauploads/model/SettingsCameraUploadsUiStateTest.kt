package mega.privacy.android.app.presentation.settings.camerauploads.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
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

    @ParameterizedTest(name = "video quality ui item: {0}")
    @EnumSource(VideoQualityUiItem::class)
    fun `test that the user cannot change the video compression charging state when photos can only be uploaded`(
        videoQualityUiItem: VideoQualityUiItem,
    ) {
        underTest = SettingsCameraUploadsUiState(
            uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
            videoQualityUiItem = videoQualityUiItem,
        )

        assertThat(underTest.canChangeChargingDuringVideoCompressionState).isFalse()
    }

    @ParameterizedTest(name = "upload option ui item: {0}")
    @EnumSource(
        value = UploadOptionUiItem::class,
        names = ["PhotosOnly"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the user cannot change the video compression charging state when videos can be uploaded and the video quality is original`(
        uploadOptionUiItem: UploadOptionUiItem,
    ) {
        underTest = SettingsCameraUploadsUiState(
            uploadOptionUiItem = uploadOptionUiItem,
            videoQualityUiItem = VideoQualityUiItem.Original,
        )

        assertThat(underTest.canChangeChargingDuringVideoCompressionState).isFalse()
    }

    @ParameterizedTest(name = "and video quality ui item is {0}")
    @EnumSource(
        value = VideoQualityUiItem::class,
        names = ["Original"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the user can change the video compression charging state when videos can only be uploaded`(
        videoQualityUiItem: VideoQualityUiItem,
    ) {
        underTest = SettingsCameraUploadsUiState(
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
            videoQualityUiItem = videoQualityUiItem,
        )

        assertThat(underTest.canChangeChargingDuringVideoCompressionState).isTrue()
    }

    @ParameterizedTest(name = "and video quality ui item is {0}")
    @EnumSource(
        value = VideoQualityUiItem::class,
        names = ["Original"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the user can change the video compression charging state when photos and videos can be uploaded`(
        videoQualityUiItem: VideoQualityUiItem,
    ) {
        underTest = SettingsCameraUploadsUiState(
            uploadOptionUiItem = UploadOptionUiItem.PhotosAndVideos,
            videoQualityUiItem = videoQualityUiItem,
        )

        assertThat(underTest.canChangeChargingDuringVideoCompressionState).isTrue()
    }

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