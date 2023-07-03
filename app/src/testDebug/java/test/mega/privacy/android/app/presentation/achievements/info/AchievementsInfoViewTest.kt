package test.mega.privacy.android.app.presentation.achievements.info

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.presentation.achievements.info.model.AchievementsInfoUIState
import mega.privacy.android.app.presentation.achievements.info.util.toAchievementsInfoAttribute
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoView
import mega.privacy.android.app.presentation.achievements.info.view.AchievementsInfoViewTestTags
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.domain.entity.achievement.AchievementType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.fromPluralId
import test.mega.privacy.android.app.hasTextColor
import kotlin.random.Random

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class AchievementsInfoViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val oneHundredMbInBytes = 104857600L
    private val achievementType = listOf(
        AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL,
        AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL,
        AchievementType.MEGA_ACHIEVEMENT_WELCOME
    )

    @Test
    fun `test that toolbar should render with correct title`() {
        composeTestRule.setContent {
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = achievementType.random(),
                    awardStorageInBytes = oneHundredMbInBytes
                )
            )
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TOOLBAR)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(fromId(R.string.achievements_title))))
    }

    @Test
    fun `test that invite friends view should render with correct main icon`() {
        val expectedType = achievementType.random()

        composeTestRule.setContent {
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = expectedType,
                    awardStorageInBytes = oneHundredMbInBytes
                )
            )
        }

        composeTestRule.onNodeWithContentDescription(expectedType.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.CHECK_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that check icon should be visible when achievement has been awarded to the user`() {
        val expectedType = achievementType.random()

        composeTestRule.setContent {
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = expectedType,
                    awardStorageInBytes = oneHundredMbInBytes,
                    isAchievementAwarded = true
                )
            )
        }

        composeTestRule.onNodeWithContentDescription(expectedType.name).assertIsDisplayed()
        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.CHECK_ICON).assertIsDisplayed()
    }

    @Test
    fun `test that title should render with correct value when achievement hasn't been awarded`() {
        val expectedType = achievementType.random()
        lateinit var context: Context

        composeTestRule.setContent {
            context = LocalContext.current
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = expectedType,
                    awardStorageInBytes = oneHundredMbInBytes,
                    isAchievementAwarded = false
                )
            )
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TITLE).assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.figures_achievements_text,
                        oneHundredMbInBytes.toUnitString(context)
                    )
                )
            )
    }

    @Test
    fun `test that title should render with correct value when achievement is not expired`() {
        val expectedType = achievementType.random()
        val expectedRemainingDays = Random.nextLong(from = 1, until = 100)

        composeTestRule.setContent {
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = expectedType,
                    awardStorageInBytes = oneHundredMbInBytes,
                    isAchievementAwarded = true,
                    isAchievementExpired = false,
                    achievementRemainingDays = expectedRemainingDays
                )
            )
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TITLE).assertIsDisplayed()
            .assert(
                hasText(
                    fromPluralId(
                        R.plurals.account_achievements_bonus_expiration_date,
                        expectedRemainingDays.toInt()
                    )
                )
            )
    }

    @Test
    fun `test that title should render with expired text when achievement is expired`() {
        val expectedType = achievementType.random()

        composeTestRule.setContent {
            AchievementsInfoView(
                modifier = Modifier,
                uiState = AchievementsInfoUIState(
                    achievementType = expectedType,
                    awardStorageInBytes = oneHundredMbInBytes,
                    isAchievementAwarded = true,
                    isAchievementExpired = true,
                    achievementRemainingDays = 0
                )
            )
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TITLE).assertIsDisplayed()
            .assert(hasText(fromId(R.string.expired_label)))
    }

    @Test
    fun `test that title color should be red_600 when achievement reward is almost expired and LIGHT theme`() {
        val expectedType = achievementType.random()

        composeTestRule.setContent {
            AndroidTheme(isDark = false) {
                AchievementsInfoView(
                    modifier = Modifier,
                    uiState = AchievementsInfoUIState(
                        achievementType = expectedType,
                        awardStorageInBytes = oneHundredMbInBytes,
                        isAchievementAwarded = true,
                        isAchievementAlmostExpired = true,
                        achievementRemainingDays = 10
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TITLE).assertIsDisplayed()
            .assert(hasTextColor(red_600))
    }

    @Test
    fun `test that title color should be red_300 when achievement reward is almost expired and DARK theme`() {
        val expectedType = achievementType.random()

        composeTestRule.setContent {
            AndroidTheme(isDark = true) {
                AchievementsInfoView(
                    modifier = Modifier,
                    uiState = AchievementsInfoUIState(
                        achievementType = expectedType,
                        awardStorageInBytes = oneHundredMbInBytes,
                        isAchievementAwarded = true,
                        isAchievementAlmostExpired = true,
                        achievementRemainingDays = 10
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.TITLE).assertIsDisplayed()
            .assert(hasTextColor(red_300))
    }

    @Test
    fun `test that subtitle should render with correct value based on achievement type`() {
        val expectedType = achievementType.random()
        val randomIsAwarded = Random.nextBoolean()
        val textResourceId =
            expectedType.toAchievementsInfoAttribute(randomIsAwarded).subtitleTextResourceId
        lateinit var context: Context

        composeTestRule.setContent {
            context = LocalContext.current
            AndroidTheme(isDark = true) {
                AchievementsInfoView(
                    modifier = Modifier,
                    uiState = AchievementsInfoUIState(
                        achievementType = expectedType,
                        awardStorageInBytes = oneHundredMbInBytes,
                        isAchievementAwarded = randomIsAwarded
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(AchievementsInfoViewTestTags.SUBTITLE).assertIsDisplayed()
            .assert(hasText(fromId(textResourceId, oneHundredMbInBytes.toUnitString(context))))
    }
}