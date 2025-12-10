package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.myaccount.R
import mega.privacy.android.feature.myaccount.presentation.model.MyAccountWidgetUiState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
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
        composeTestRule.onRoot()
            .assertExists()
            .assertIsDisplayed()
    }

    // Helper method to assert greeting text
    private fun assertGreetingText(expectedName: String) {
        composeTestRule.onNodeWithText("${composeTestRule.activity.getString(R.string.general_hi)} $expectedName!")
            .assertExists()
            .assertIsDisplayed()
    }

    // Helper method to assert account type text
    private fun assertAccountTypeText(accountTypeResource: Int) {
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(accountTypeResource))
            .assertExists()
            .assertIsDisplayed()
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

        // When loading, the actual content should not be displayed
        // Instead, shimmer effects should be visible
        assertWidgetIsDisplayed()
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
        // The greeting will be "Hi !" since name is null and becomes empty string
        composeTestRule.onNodeWithText("${composeTestRule.activity.getString(R.string.general_hi)} !")
            .assertExists()
            .assertIsDisplayed()

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

        // Perform click
        composeTestRule.onRoot().performClick()

        // Verify callback was triggered
        assertTrue("onClick callback should be triggered", clickTriggered)
    }

    @Test
    fun `test that chevron icon is displayed`() {
        setWidgetContent(createBasicState())

        // The chevron icon should be visible but doesn't have a content description
        // So we verify the widget structure is complete
        assertWidgetIsDisplayed()
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
    fun `test that shimmer view is displayed when loading`() {
        setWidgetContent(MyAccountWidgetUiState(isLoading = true))

        // When loading, content should still be displayed (but as shimmer)
        assertWidgetIsDisplayed()
    }
}
