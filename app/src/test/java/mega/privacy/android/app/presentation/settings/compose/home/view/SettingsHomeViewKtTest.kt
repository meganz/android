package mega.privacy.android.app.presentation.settings.compose.home.view

import android.os.Parcelable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.compose.home.model.MyAccountSettingsState
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.mobile.analytics.event.QASettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.SettingsScreenEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsHomeViewKtTest {

    val composeTestRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that screen view event is tracked`() {
        composeTestRule.setContent {
            SettingsHomeView(
                state = SettingsHomeState.Loading(
                    featureEntryPoints = persistentListOf(),
                    moreEntryPoints = persistentListOf(),
                ),
                onBackPressed = {},
                initialScreen = null,
            )
        }

        assertThat(analyticsRule.events).contains(SettingsScreenEvent)
    }

    @Test
    fun `test that analytics are called when settings item is selected`() {

        composeTestRule.setContent {
            SettingsHomeView(
                state = SettingsHomeState.Data(
                    myAccountState = MyAccountSettingsState(UserId(12L), "", ""),
                    featureEntryPoints = persistentListOf(
                        FeatureSettingEntryPoint(
                            key = "", //Empty key sets the test tag equal to just the root tag
                            title = R.string.settings_file_management_category,
                            icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
                            preferredOrdinal = 0,
                            destination = TestDestination,
                            analyticsEvent = QASettingsItemSelectedEvent
                        )
                    ),
                    moreEntryPoints = persistentListOf()
                ),
                onBackPressed = {},
                initialScreen = null,
            )
        }

        composeTestRule.onNodeWithTag(SETTING_ITEM_TEST_TAG_ROOT).performClick()
        assertThat(analyticsRule.events).contains(QASettingsItemSelectedEvent)
    }

    @Parcelize
    object TestDestination : Parcelable

}