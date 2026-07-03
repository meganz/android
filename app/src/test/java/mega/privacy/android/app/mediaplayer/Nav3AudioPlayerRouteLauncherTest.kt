package mega.privacy.android.app.mediaplayer

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.navigation.AudioPlayerScreenNavKey
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

// ComponentName is a final class in Android SDK — its constructor is a stub in JVM unit tests
// (does not store arguments), so getClassName() always returns null. To avoid this, the
// Android-dependent check is extracted to targetsAudioPlayer() which is stubbed via spy.

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class Nav3AudioPlayerRouteLauncherTest {

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private var launchSourceHolder = AudioPlayerLaunchSourceHolder()
    private var underTest = spy(
        Nav3AudioPlayerRouteLauncher(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            launchSourceHolder = launchSourceHolder,
        )
    )

    private val audioPlayerIntent: Intent = mock()
    private val otherIntent: Intent = mock()

    @BeforeEach
    fun resetMocks() {
        reset(getFeatureFlagValueUseCase)
        launchSourceHolder = AudioPlayerLaunchSourceHolder()
        underTest = spy(
            Nav3AudioPlayerRouteLauncher(
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                launchSourceHolder = launchSourceHolder,
            )
        )
        doReturn(true).whenever(underTest).targetsAudioPlayer(audioPlayerIntent)
        doReturn(false).whenever(underTest).targetsAudioPlayer(otherIntent)
    }

    @Test
    fun `test that routeOrNull returns null when feature flag is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)).thenReturn(false)

        val result = underTest.routeOrNull(audioPlayerIntent)

        assertThat(result).isNull()
    }

    @Test
    fun `test that routeOrNull returns null when feature flag check throws`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp))
            .thenThrow(RuntimeException("flag error"))

        val result = underTest.routeOrNull(audioPlayerIntent)

        assertThat(result).isNull()
    }

    @Test
    fun `test that routeOrNull returns null when intent does not target audio player`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)).thenReturn(true)

        val result = underTest.routeOrNull(otherIntent)

        assertThat(result).isNull()
    }

    @Test
    fun `test that routeOrNull returns AudioPlayerScreenNavKey when flag is enabled and component matches`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)).thenReturn(true)

            val result = underTest.routeOrNull(audioPlayerIntent)

            assertThat(result).isInstanceOf(AudioPlayerScreenNavKey::class.java)
        }

    @Test
    fun `test that routeOrNull stashes intent in holder when routing succeeds`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)).thenReturn(true)

        val key = underTest.routeOrNull(audioPlayerIntent) as AudioPlayerScreenNavKey

        assertThat(launchSourceHolder.consume(key.launchId)).isEqualTo(audioPlayerIntent)
    }

    @Test
    fun `test that targetsAudioPlayer returns false when component class name is null`() {
        val result = Nav3AudioPlayerRouteLauncher(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            launchSourceHolder = launchSourceHolder,
        ).targetsAudioPlayer(audioPlayerIntent)

        // In JVM unit tests ComponentName.getClassName() is a stub returning null,
        // so this validates the false branch — instrumented tests cover the true branch.
        assertThat(result).isFalse()
    }

    @Test
    fun `test that consume clears the stored intent`() {
        launchSourceHolder.put("id", mock())
        launchSourceHolder.consume("id")
        assertThat(launchSourceHolder.consume("id")).isNull()
    }
}
