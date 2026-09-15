package mega.privacy.android.navigation.contract.menu

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class CommonMenuActionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that Delete is described by the shared delete string`() {
        var description: String? = null
        composeTestRule.setContent {
            description = CommonMenuAction.Delete.getDescription()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(description).isEqualTo(context.getString(sharedR.string.general_delete))
    }
}
