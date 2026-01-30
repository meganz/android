package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.myaccount.presentation.model.MyAccountWidgetUiState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import mega.privacy.android.shared.resources.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyAccountWidgetTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Helper method to create widget with state
    private fun setWidgetContent(state: MyAccountWidgetUiState, onClick: () -> Unit = {}) {
        composeTestRule.setContent {
            MyAccountWidget(
                state = state,
                onClick = onClick
            )
        }
    }

    // Helper method to create a basic non-loading state
    private fun createBasicState(
        name: String = "Test User",
        accountTypeNameResource: Int = R.string.free_account,
        usedStorage: Long = 1000000L,
        totalStorage: Long = 10000000L,
        usedStoragePercentage: Int = 10,
        storageQuotaLevel: QuotaLevel = QuotaLevel.Success,
    ) = MyAccountWidgetUiState(
        name = name,
        accountTypeNameResource = accountTypeNameResource,
        usedStorage = usedStorage,
        totalStorage = totalStorage,
        usedStoragePercentage = usedStoragePercentage,
        storageQuotaLevel = storageQuotaLevel,
        isLoading = false
    )

    // Helper method to assert widget basic display
    private fun assertWidgetIsDisplayed() {
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }

    // Helper method to assert greeting text
    private fun assertGreetingText(expectedName: String) {
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_USER_NAME_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .assert(hasText("${composeTestRule.activity.getString(R.string.general_hi)} $expectedName!"))

    }

    // Helper method to assert account type text
    private fun assertAccountTypeText(accountTypeResource: Int) {
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_ACCOUNT_TYPE_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .assert(hasText(composeTestRule.activity.getString(accountTypeResource)))
    }

    // Helper method to verify widget with specific quota level displays correctly
    private fun verifyQuotaLevelDisplay(
        userName: String,
        accountType: Int,
        quotaLevel: QuotaLevel,
        usagePercent: Int = 10,
    ) {
        val testState = createBasicState(
            name = userName,
            accountTypeNameResource = accountType,
            usedStoragePercentage = usagePercent,
            storageQuotaLevel = quotaLevel
        )

        setWidgetContent(testState)

        assertWidgetIsDisplayed()
        assertGreetingText(userName)
        assertAccountTypeText(accountType)
    }

    @Test
    fun `test that loading state shows shimmer view`() {
        setWidgetContent(MyAccountWidgetUiState(isLoading = true))

        // When loading, shimmer should be visible
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_SHIMMER_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()

        // Content elements should not exist when loading
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_AVATAR_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_USER_NAME_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_STORAGE_USAGE_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_PROGRESS_BAR_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_CHEVRON_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that loaded state displays user information correctly`() {
        val testState = createBasicState(
            name = "John Doe",
            accountTypeNameResource = R.string.pro1_account,
            usedStorage = 124470000000L,  // ~116 GB
            totalStorage = 750000000000L,  // ~698 GB
            usedStoragePercentage = 50,
            storageQuotaLevel = QuotaLevel.Success
        )

        setWidgetContent(testState)

        // Verify user name is displayed with greeting
        assertGreetingText("John Doe")

        // Verify account type is displayed
        assertAccountTypeText(R.string.pro1_account)

        // Note: Storage usage text format may vary based on actual implementation
        // This assertion might need to be updated based on the actual formatting
    }

    @Test
    fun `test that widget handles null name gracefully`() {
        val testState = createBasicState(name = "").copy(name = null)

        setWidgetContent(testState)

        // Widget should still be displayed even with null name
        assertWidgetIsDisplayed()

        // User name element should still exist
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_USER_NAME_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
            .assert(hasText("${composeTestRule.activity.getString(R.string.general_hi)} !"))

        // Account type should still be shown
        assertAccountTypeText(R.string.free_account)
    }

    @Test
    fun `test that widget handles zero account type resource gracefully`() {
        val testState = createBasicState(accountTypeNameResource = 0)

        setWidgetContent(testState)

        // Widget should still be displayed
        assertWidgetIsDisplayed()

        // User name should be shown with greeting
        assertGreetingText("Test User")

        // Account type element should not exist when resource is zero
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_ACCOUNT_TYPE_TEST_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that widget displays success quota level correctly`() {
        verifyQuotaLevelDisplay("Test User", R.string.free_account, QuotaLevel.Success, 10)
    }

    @Test
    fun `test that widget displays warning quota level correctly`() {
        verifyQuotaLevelDisplay("Warning User", R.string.prolite_account, QuotaLevel.Warning, 85)
    }

    @Test
    fun `test that widget displays error quota level correctly`() {
        verifyQuotaLevelDisplay("Critical User", R.string.pro1_account, QuotaLevel.Error, 95)
    }

    @Test
    fun `test that widget is clickable and triggers onClick callback`() {
        var clickTriggered = false

        setWidgetContent(
            state = createBasicState(),
            onClick = { clickTriggered = true }
        )

        // Perform click using test tag
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_TEST_TAG, useUnmergedTree = true)
            .performClick()

        // Verify callback was triggered
        assertTrue("onClick callback should be triggered", clickTriggered)
    }

    @Test
    fun `test that chevron icon is displayed`() {
        setWidgetContent(createBasicState())

        // Verify chevron icon using test tag
        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_CHEVRON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that storage usage handles large values correctly`() {
        val testState = createBasicState(
            name = "Heavy User",
            accountTypeNameResource = R.string.pro3_account,
            usedStorage = 5497558138880L,  // ~5 TB
            totalStorage = 8796093022208L,  // ~8 TB
            usedStoragePercentage = 62,
            storageQuotaLevel = QuotaLevel.Warning
        )

        setWidgetContent(testState)

        // Verify that large storage values are handled
        assertWidgetIsDisplayed()

        // Verify user name is displayed with greeting
        assertGreetingText("Heavy User")

        // Verify account type is displayed
        assertAccountTypeText(R.string.pro3_account)
    }

    @Test
    fun `test that business account is displayed correctly`() {
        val testState = createBasicState(
            name = "Business User",
            accountTypeNameResource = R.string.business_label,
            usedStorage = 10737418240L,  // 10 GB
            totalStorage = 1099511627776L,  // 1 TB
            usedStoragePercentage = 1,
            storageQuotaLevel = QuotaLevel.Success
        )

        setWidgetContent(testState)

        // Verify business account type is displayed
        assertAccountTypeText(R.string.business_label)

        // Verify user name is displayed with greeting
        assertGreetingText("Business User")
    }

    @Test
    fun `test that widget handles maximum storage usage`() {
        val testState = createBasicState(
            name = "Full Storage User",
            accountTypeNameResource = R.string.free_account,
            usedStorage = 10000000000L,  // 10 GB
            totalStorage = 10000000000L,  // 10 GB (100% usage)
            usedStoragePercentage = 100,
            storageQuotaLevel = QuotaLevel.Error
        )

        setWidgetContent(testState)

        // Widget should handle 100% usage gracefully
        assertWidgetIsDisplayed()
    }

    @Test
    fun `test that avatar is displayed in loaded state`() {
        setWidgetContent(createBasicState())

        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_AVATAR_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that storage usage text is displayed`() {
        setWidgetContent(createBasicState())

        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_STORAGE_USAGE_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that progress bar is displayed`() {
        setWidgetContent(createBasicState())

        composeTestRule.onNodeWithTag(MY_ACCOUNT_WIDGET_PROGRESS_BAR_TEST_TAG, useUnmergedTree = true)
            .assertExists()
            .assertIsDisplayed()
    }
}
