package mega.privacy.android.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.gateway.FEATURE_FLAG_PREFERENCES
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QAModuleTest {

    private val underTest = QAModule()

    @Test
    fun `test that provideFeatureFlagPreferencesFileName returns the DataStore file name without resolving its path`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expected = context.preferencesDataStoreFile(FEATURE_FLAG_PREFERENCES).name

        assertThat(underTest.provideFeatureFlagPreferencesFileName()).isEqualTo(expected)
    }
}
