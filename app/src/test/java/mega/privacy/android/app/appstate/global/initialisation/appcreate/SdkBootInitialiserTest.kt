package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.NativeLibraryLoader
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SdkBootInitialiserTest {

    private val enableLogAllToConsole = mock<EnableLogAllToConsole>()

    private val underTest = SdkBootInitialiser(
        enableLogAllToConsole = { enableLogAllToConsole },
    )

    @BeforeEach
    fun setUp() {
        NativeLibraryLoader.markLoadedForTest()
    }

    @Test
    fun `test that invoke attaches the console loggers once the native library is loaded`() =
        runTest {
            underTest()

            verify(enableLogAllToConsole).invoke(BuildConfig.DEBUG)
        }
}
