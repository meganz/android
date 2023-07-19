package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.RK_EXPORT_BOX_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.RecoveryKeyBox
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1080dp-h1920dp")
class RecoveryKeyBoxTest {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule() {
        composeRule.setContent {
            RecoveryKeyBox(
                testTag = RK_EXPORT_BOX_TEST_TAG,
                onExportRkClicked = {},
            )
        }
    }

    @Test
    fun `test that recovery key file name is shown`() {
        setupRule()
        val rkFileName = "${fromId(R.string.general_rk)}.txt"
        composeRule.onNodeWithText(rkFileName).assertIsDisplayed()
    }
}