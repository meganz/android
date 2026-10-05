package mega.privacy.android.app.appstate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.appstate.MegaActivityInternalLauncher.INTERNAL_ENTRY_COMPONENT
import mega.privacy.android.app.appstate.MegaActivityInternalLauncher.isInternalEntry
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class MegaActivityIntentDestinationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `test that getIntentWithExtraDestinations targets the internal entry component`() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            context,
            listOf(CustomiseNavigationNavKey())
        )

        assertThat(intent.component?.className).isEqualTo(INTERNAL_ENTRY_COMPONENT)
        assertThat(intent.component?.packageName).isEqualTo(context.packageName)
    }

    @Test
    fun `test that getIntentWithExtraDestinations keeps single top and clear top flags`() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            context,
            listOf(CustomiseNavigationNavKey())
        )

        assertThat(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP).isNotEqualTo(0)
    }

    @Test
    fun `test that isInternalEntry returns true when intent targets the internal entry component`() {
        val intent = Intent().setComponent(ComponentName(context, INTERNAL_ENTRY_COMPONENT))

        assertThat(intent.isInternalEntry()).isTrue()
    }

    @Test
    fun `test that isInternalEntry returns false when intent targets the exported MegaActivity`() {
        val intent = Intent(context, MegaActivity::class.java)

        assertThat(intent.isInternalEntry()).isFalse()
    }

    @Test
    fun `test that isInternalEntry returns false when intent has no component`() {
        assertThat(Intent().isInternalEntry()).isFalse()
    }
}
