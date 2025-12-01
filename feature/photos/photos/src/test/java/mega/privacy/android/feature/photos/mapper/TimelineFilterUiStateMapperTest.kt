package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineFilterUiStateMapperTest {

    private lateinit var underTest: TimelineFilterUiStateMapper

    @BeforeEach
    fun setup() {
        underTest = TimelineFilterUiStateMapper()
    }

    @Test
    fun `test that the default state is returned when the preference map is NULL`() {
        val actual = underTest(
            preferenceMap = null,
            shouldApplyFilterFromPreference = false
        )

        assertThat(actual).isEqualTo(TimelineFilterUiState())
    }

    @Test
    fun `test that the default state is returned when the preferences are not remembered and shouldn't apply the filter from preference`() {
        val preferenceMap = mapOf(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "false"
        )

        val actual = underTest(
            preferenceMap = preferenceMap,
            shouldApplyFilterFromPreference = false
        )

        assertThat(actual).isEqualTo(TimelineFilterUiState())
    }

    @Test
    fun `test that true is set as the remember value`() {
        val preferenceMap = mapOf(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "true"
        )

        val actual = underTest(
            preferenceMap = preferenceMap,
            shouldApplyFilterFromPreference = false
        )

        assertThat(actual.isRemembered).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideMediaType")
    fun `test that the correct filter media type is returned when remembered`(mediaType: String?) {
        val preferenceMap = mapOf(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "true",
            TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType
        )

        val actual = underTest(
            preferenceMap = preferenceMap,
            shouldApplyFilterFromPreference = false
        )

        val expected = when (mediaType) {
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value -> FilterMediaType.ALL_MEDIA
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> FilterMediaType.IMAGES
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> FilterMediaType.VIDEOS
            else -> FilterMediaType.DEFAULT
        }
        assertThat(actual.mediaType).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideMediaType")
    fun `test that the correct filter media type is returned when should apply filter from preference even though the filter is not remembered`(
        mediaType: String?,
    ) {
        val preferenceMap = mapOf(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "false",
            TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType
        )

        val actual = underTest(
            preferenceMap = preferenceMap,
            shouldApplyFilterFromPreference = true
        )

        val expected = when (mediaType) {
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value -> FilterMediaType.ALL_MEDIA
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> FilterMediaType.IMAGES
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> FilterMediaType.VIDEOS
            else -> FilterMediaType.DEFAULT
        }
        assertThat(actual.mediaType).isEqualTo(expected)
    }

    private fun provideMediaType() = Stream.of(
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value),
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value),
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value),
        Arguments.of(null),
    )

    @ParameterizedTest
    @MethodSource("provideMediaSource")
    fun `test that the correct filter media source is returned when should apply filter from preference even though the filter is not remembered`(
        mediaSource: String?,
    ) {
        val preferenceMap = mapOf(
            TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "false",
            TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource
        )

        val actual = underTest(
            preferenceMap = preferenceMap,
            shouldApplyFilterFromPreference = true
        )

        val expected = when (mediaSource) {
            TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> FilterMediaSource.AllPhotos
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> FilterMediaSource.CloudDrive
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> FilterMediaSource.CameraUpload
            else -> FilterMediaSource.AllPhotos
        }
        assertThat(actual.mediaSource).isEqualTo(expected)
    }

    private fun provideMediaSource() = Stream.of(
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value),
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value),
        Arguments.of(TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value),
        Arguments.of(null),
    )
}
