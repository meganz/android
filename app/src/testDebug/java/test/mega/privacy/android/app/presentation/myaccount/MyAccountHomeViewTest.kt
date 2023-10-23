package test.mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.MyAccountHomeViewActions
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.app.presentation.myaccount.view.AccountTypeSection
import mega.privacy.android.app.presentation.myaccount.view.Constants.ACCOUNT_TYPE_SECTION
import mega.privacy.android.app.presentation.myaccount.view.Constants.ACHIEVEMENTS
import mega.privacy.android.app.presentation.myaccount.view.Constants.ADD_PHONE_NUMBER
import mega.privacy.android.app.presentation.myaccount.view.Constants.AVATAR_SIZE
import mega.privacy.android.app.presentation.myaccount.view.Constants.BACKUP_RECOVERY_KEY
import mega.privacy.android.app.presentation.myaccount.view.Constants.CONTACTS
import mega.privacy.android.app.presentation.myaccount.view.Constants.CONTAINER_LEFT_MARGIN
import mega.privacy.android.app.presentation.myaccount.view.Constants.EMAIL_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.EXPIRED_BUSINESS_BANNER
import mega.privacy.android.app.presentation.myaccount.view.Constants.EXPIRED_BUSINESS_BANNER_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.HEADER_LEFT_MARGIN
import mega.privacy.android.app.presentation.myaccount.view.Constants.HEADER_RIGHT_MARGIN
import mega.privacy.android.app.presentation.myaccount.view.Constants.IMAGE_AVATAR
import mega.privacy.android.app.presentation.myaccount.view.Constants.LAST_SESSION
import mega.privacy.android.app.presentation.myaccount.view.Constants.NAME_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.PAYMENT_ALERT_INFO
import mega.privacy.android.app.presentation.myaccount.view.Constants.PHONE_NUMBER_TEXT
import mega.privacy.android.app.presentation.myaccount.view.Constants.TEXT_AVATAR
import mega.privacy.android.app.presentation.myaccount.view.Constants.UPGRADE_BUTTON
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_STORAGE_IMAGE
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_STORAGE_PROGRESS
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_IMAGE
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_PROGRESS
import mega.privacy.android.app.presentation.myaccount.view.Constants.USAGE_TRANSFER_SECTION
import mega.privacy.android.app.presentation.myaccount.view.MyAccountHeader
import mega.privacy.android.app.presentation.myaccount.view.MyAccountHomeView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.AccountType.*
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.fromPluralId
import kotlin.random.Random

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MyAccountHomeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    private fun initNavHostController() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun initMyAccountWithDefaults(uiState: MyAccountHomeUIState = MyAccountHomeUIState()) {
        composeTestRule.setContent {
            initNavHostController()

            MyAccountHomeView(
                uiState = uiState,
                uiActions = object : MyAccountHomeViewActions {
                    override val isPhoneNumberDialogShown: Boolean
                        get() = false
                },
                navController = navController
            )
        }
    }

    @Test
    fun `test that my account header section should render with correct attributes`() {
        composeTestRule.setContent {
            MyAccountHeader(
                avatar = null,
                avatarColor = R.color.dark_grey,
                name = "Mega",
                email = "asd@mega.co.nz",
                verifiedPhoneNumber = "123456789",
                onClickUserAvatar = {},
                onEditProfile = {}
            )
        }

        composeTestRule.onNodeWithTag(IMAGE_AVATAR).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEXT_AVATAR)
            .assertIsDisplayed()
            .assert(hasText("M"))
        composeTestRule.onNodeWithTag(NAME_TEXT)
            .assertIsDisplayed()
            .assert(hasText("Mega"))
        composeTestRule.onNodeWithTag(EMAIL_TEXT)
            .assertIsDisplayed()
            .assert(hasText("asd@mega.co.nz"))
        composeTestRule.onNodeWithTag(PHONE_NUMBER_TEXT)
            .assertIsDisplayed()
            .assert(hasText("123456789"))
    }

    @Test
    fun `test that account type section render with correct attributes when account is FREE`() {
        verifyAccountTypeSection(FREE, R.string.free_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is PRO_LITE`() {
        verifyAccountTypeSection(PRO_LITE, R.string.prolite_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is PRO I`() {
        verifyAccountTypeSection(PRO_I, R.string.pro1_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is PRO II`() {
        verifyAccountTypeSection(PRO_II, R.string.pro2_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is PRO III`() {
        verifyAccountTypeSection(PRO_III, R.string.pro3_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is PRO Flexi`() {
        verifyAccountTypeSection(PRO_FLEXI, R.string.pro_flexi_account)
    }

    @Test
    fun `test that account type section render with correct attributes when account is Business`() {
        verifyAccountTypeSection(BUSINESS, R.string.business_label)
    }

    @Test
    fun `test that account type button should be invisible when account is BUSINESS account`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                accountType = BUSINESS,
                isBusinessAccount = true,
                isMasterBusinessAccount = true
            )
        )

        composeTestRule.onNodeWithTag(UPGRADE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that account type button should be invisible when account is PRO_FLEXI account`() {
        initMyAccountWithDefaults(MyAccountHomeUIState(accountType = PRO_FLEXI))

        composeTestRule.onNodeWithTag(UPGRADE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that account type button text should render with correct text when account not business account`() {
        initMyAccountWithDefaults(MyAccountHomeUIState(accountType = PRO_I))

        composeTestRule.onNodeWithTag(UPGRADE_BUTTON).assertIsDisplayed()
            .assert(hasText(fromId(R.string.my_account_upgrade_pro)))
    }

    @Test
    fun `test that expired banner should be visible when account is master business account but is currently expired or on grace period`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = false
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER).assertIsDisplayed()
    }

    @Test
    fun `test that expired banner should render with correct text when expired`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = false,
                businessStatus = BusinessAccountStatus.Expired
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER_TEXT)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.payment_overdue_label)))
    }

    @Test
    fun `test that expired banner should render with correct text when on grace period`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = false,
                businessStatus = BusinessAccountStatus.GracePeriod
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER_TEXT)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.payment_required_label)))
    }

    @Test
    fun `test that expired banner should not be shown when business status is active`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = true,
                businessStatus = BusinessAccountStatus.Active
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER).assertDoesNotExist()
    }

    @Test
    fun `test that payment alert should be visible when business status active and has renewable or expireable subscription`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = true,
                hasRenewableSubscription = true,
                hasExpireAbleSubscription = true
            )
        )

        composeTestRule.onNodeWithTag(PAYMENT_ALERT_INFO).assertIsDisplayed()
    }

    @Test
    fun `test that payment alert should be visible when account is not BUSINESS and has renewable or expireable subscription`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                hasRenewableSubscription = true,
                hasExpireAbleSubscription = true
            )
        )

        composeTestRule.onNodeWithTag(PAYMENT_ALERT_INFO).assertIsDisplayed()
    }

    @Test
    fun `test that payment alert should be invisible when account has no renewable or expireable subscription`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                hasRenewableSubscription = false,
                hasExpireAbleSubscription = false
            )
        )

        composeTestRule.onNodeWithTag(PAYMENT_ALERT_INFO).assertDoesNotExist()
    }

    @Test
    fun `test that usage transfer should be invisible when account type is FREE`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                accountType = FREE
            )
        )

        composeTestRule.onNodeWithTag(USAGE_TRANSFER_SECTION).assertDoesNotExist()
    }

    @Test
    fun `test that usage transfer should be visible when account type is not FREE`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(accountType = PRO_I)
        )

        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_SECTION, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that usage progress bar should be invisible and show image instead when account is BUSINESS`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isBusinessAccount = true,
                accountType = BUSINESS
            )
        )

        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_PROGRESS, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_PROGRESS, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that usage progress bar should be invisible and show image instead when account is PRO_FLEXI`() {
        initMyAccountWithDefaults(MyAccountHomeUIState(accountType = PRO_FLEXI))

        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_PROGRESS, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_PROGRESS, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that usage progress bar should be visible when account is other than BUSINESS or PRO_FLEXI`() {
        val randomAccountType = enumValues<AccountType>()
            .filter { it != BUSINESS }
            .filter { it != PRO_FLEXI }
            .filter { it != FREE }
            .random()
        initMyAccountWithDefaults(MyAccountHomeUIState(accountType = randomAccountType))

        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_PROGRESS, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_PROGRESS, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = USAGE_STORAGE_IMAGE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = USAGE_TRANSFER_IMAGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that my account menus should be visible on first load and default state`() {
        initMyAccountWithDefaults()

        composeTestRule.onNodeWithTag(ADD_PHONE_NUMBER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(BACKUP_RECOVERY_KEY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACTS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ACHIEVEMENTS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LAST_SESSION).assertIsDisplayed()
    }

    @Test
    fun `test that add phone number menu should be visible when phone number can be verified`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                canVerifyPhoneNumber = true,
                verifiedPhoneNumber = null
            )
        )

        composeTestRule.onNodeWithTag(ADD_PHONE_NUMBER).assertIsDisplayed()
    }

    @Test
    fun `test that add phone number section should be invisible when phone number existed`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                canVerifyPhoneNumber = false,
                verifiedPhoneNumber = "1231231231"
            )
        )

        composeTestRule.onNodeWithTag(ADD_PHONE_NUMBER).assertDoesNotExist()
    }

    @Test
    fun `test that contacts should show correct number of contacts when screen loads`() {
        val randomContact = Random.nextInt(from = 1, until = 100)

        initMyAccountWithDefaults(
            MyAccountHomeUIState(visibleContacts = randomContact)
        )

        composeTestRule.onNodeWithTag(testTag = "${CONTACTS}_description", useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(hasText(fromPluralId(R.plurals.my_account_connections, randomContact)))
    }

    @Test
    fun `test that achievements should not be visible if account type is BUSINESS`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(isBusinessAccount = true, accountType = BUSINESS)
        )

        composeTestRule.onNodeWithTag(ACHIEVEMENTS).assertDoesNotExist()
    }

    @Test
    fun `test that add your phone number should render with correct subtitle`() {
        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                canVerifyPhoneNumber = true,
            )
        )

        composeTestRule.onNodeWithTag(ADD_PHONE_NUMBER)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.sms_add_phone_number_dialog_msg_non_achievement_user)))
    }

    @Test
    fun `test that expire or grace alert should not be shown when due is more than 7 days`() {
        val eightDaysInSeconds = 691200
        val dueDateInSeconds = (System.currentTimeMillis() / 1000) + eightDaysInSeconds

        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = false,
                hasRenewableSubscription = true,
                subscriptionRenewTime = dueDateInSeconds
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER).assertDoesNotExist()
    }

    @Test
    fun `test that expire or grace alert should be shown when due is less than 7 days`() {
        val fiveDaysInSeconds = 432000
        val dueDateInSeconds = (System.currentTimeMillis() / 1000) + fiveDaysInSeconds

        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isMasterBusinessAccount = true,
                isBusinessStatusActive = false,
                hasRenewableSubscription = true,
                subscriptionRenewTime = dueDateInSeconds
            )
        )

        composeTestRule.onNodeWithTag(EXPIRED_BUSINESS_BANNER).assertIsDisplayed()
    }
    @Test
    fun `test that payment alert should not be shown when due is more than 7 days`() {
        val eightDaysInSeconds = 691200
        val dueDateInSeconds = (System.currentTimeMillis() / 1000) + eightDaysInSeconds

        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isBusinessAccount = false,
                hasRenewableSubscription = true,
                subscriptionRenewTime = dueDateInSeconds
            )
        )

        composeTestRule.onNodeWithTag(PAYMENT_ALERT_INFO).assertDoesNotExist()
    }

    @Test
    fun `test that payment alert should be shown when due is less than 7 days`() {
        val fiveDaysInSeconds = 432000
        val dueDateInSeconds = (System.currentTimeMillis() / 1000) + fiveDaysInSeconds

        initMyAccountWithDefaults(
            MyAccountHomeUIState(
                isBusinessAccount = false,
                hasRenewableSubscription = true,
                subscriptionRenewTime = dueDateInSeconds
            )
        )

        composeTestRule.onNodeWithTag(PAYMENT_ALERT_INFO).assertIsDisplayed()
    }

    @Test
    fun `test that name text should not exceed bounds when name is too long`() {
        val sampleText =
            "JAKJSAKLSJAKLSJAKLSJAKLJBRIJBNDUQWBDYUIWBDIBNDJKSBDSHJKBDDBWI*EBWUIBNJASBDJSBDIBQBAJSBJAKSAJSHJAKSHJAKHSJKAHSJHJKAHSJKAHS"
        val maxScreenWidth = 720.dp

        initMyAccountWithDefaults(
            MyAccountHomeUIState(name = sampleText)
        )

        val maxWidth = composeTestRule.onNodeWithTag(NAME_TEXT)
            .assertIsDisplayed()
            .assertTextEquals(sampleText)
            .getBoundsInRoot()
            .width


        assertThat(maxWidth)
            .isAtMost(maxScreenWidth - CONTAINER_LEFT_MARGIN - AVATAR_SIZE.dp - HEADER_LEFT_MARGIN - HEADER_RIGHT_MARGIN)
    }

    private fun verifyAccountTypeSection(
        accountType: AccountType,
        accountName: Int,
    ) {
        composeTestRule.setContent {
            AccountTypeSection(
                accountType = accountType,
                showButton = true,
                onButtonClickListener = {}
            )
        }

        composeTestRule.onNodeWithTag(ACCOUNT_TYPE_SECTION)
            .assertIsDisplayed().assert(hasAnyChild(hasText(fromId(accountName))))
    }
}