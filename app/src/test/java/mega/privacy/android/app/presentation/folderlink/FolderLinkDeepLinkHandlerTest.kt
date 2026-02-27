package mega.privacy.android.app.presentation.folderlink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderLinkDeepLinkHandlerTest {
    private lateinit var underTest: FolderLinkDeepLinkHandler
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setup() {
        underTest = FolderLinkDeepLinkHandler(mock(), getFeatureFlagValueUseCase)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that FolderLinkNavKey is returned when FolderLinkRevamp feature flag is enabled and regex matches`(
        isLoggedIn: Boolean,
    ) = runTest {
        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.FolderLinkRevamp) }.thenReturn(
            true
        )
        val uriString = "https://mega.co/folder"
        val expected = FolderLinkNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FOLDER_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that LegacyFolderLinkNavKey is returned when FolderLinkRevamp feature flag is disabled and regex matches`(
        isLoggedIn: Boolean,
    ) = runTest {
        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.FolderLinkRevamp) }.thenReturn(
            false
        )
        val uriString = "https://mega.co/folder"
        val expected = LegacyFolderLinkNavKey(uriString)
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FOLDER_LINK, isLoggedIn)

        assertThat(actual).containsExactly(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when the uri does not match regex pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.co/folder"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
    }
}

