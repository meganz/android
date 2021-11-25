package mega.privacy.android.app

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/**
 * Example test case using Roboletric framework.
 *
 * 22-11-2021 For now below case always fail due to tight coupling in MEGA code.
 * So below code is fully commented out, only to show for the purpose of demo code.
 * Next step options:
 * 1) mock the dependencies in test case
 * 2) refactor MEGA code to reduce the dependency
 */
/*
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q], manifest = Config.NONE)
class RoboletricExampleUnitTest {

    @Test
    fun test_readStringFromContext_LocalizedString() {
        val mockContext: Context = ApplicationProvider.getApplicationContext()

        val appName: String = mockContext.getString(R.string.app_name)
        assertThat(appName).isEqualTo("MEGA")

        val proLiteName: String = mockContext.getString(R.string.prolite_account)
        assertThat(proLiteName).isEqualTo("Pro Lite")
    }
}*/
