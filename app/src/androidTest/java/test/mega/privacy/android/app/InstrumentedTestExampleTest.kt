package test.mega.privacy.android.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentedTestExampleTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun test_readPackageIdFromContext() {
        assertThat(appContext.packageName).isEqualTo("mega.privacy.android.app")
    }

    @Test
    fun test_readStringFromContext_LocalizedString() {
        assertThat(appContext.getString(R.string.app_name)).isEqualTo("MEGA")

        assertThat(appContext.getString(R.string.prolite_account)).isEqualTo("Pro Lite")
    }
}