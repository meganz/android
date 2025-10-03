package mega.privacy.android.app.menu.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.menu.navigation.AchievementsItem
import mega.privacy.android.app.menu.navigation.CurrentPlanItem
import mega.privacy.android.app.menu.navigation.RubbishBinItem
import mega.privacy.android.app.menu.navigation.StorageItem
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.NavDrawerItem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MenuViewModelTest {

    private lateinit var underTest: MenuViewModel
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val getMyAvatarFileUseCase = mock<GetMyAvatarFileUseCase>()
    private val accountNameMapper = mock<AccountNameMapper>()
    private val getStringFromStringResMapper = mock<GetStringFromStringResMapper>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val getUserFullNameUseCase = mock<GetUserFullNameUseCase>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val isAchievementsEnabledUseCase = mock<IsAchievementsEnabledUseCase>()
    private val checkPasswordReminderUseCase = mock<CheckPasswordReminderUseCase>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    private object TestDestination : NavKey

    @BeforeAll
    fun initialisation() {
        Dispatchers.setMain(ioDispatcher)
    }

    @AfterAll
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        reset(
            monitorConnectivityUseCase,
            monitorAccountDetailUseCase,
            monitorMyAvatarFile,
            getMyAvatarColorUseCase,
            getMyAvatarFileUseCase,
            accountNameMapper,
            getStringFromStringResMapper,
            fileSizeStringMapper,
            getUserFullNameUseCase,
            getCurrentUserEmail,
            monitorUserUpdates,
            isAchievementsEnabledUseCase,
            checkPasswordReminderUseCase
        )
    }

    @Test
    fun `test that initial state contains empty menu items`() = runTest {
        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(flowOf(false))
        }
        initUnderTest()

        val initialState = underTest.uiState.value
        assertThat(initialState.myAccountItems).isEmpty()
        assertThat(initialState.privacySuiteItems).isEmpty()
        assertThat(initialState.email).isNull()
        assertThat(initialState.name).isNull()
        assertThat(initialState.avatar).isNull()
        assertThat(initialState.isConnectedToNetwork).isFalse()
    }

    @Test
    fun `test that menu items are filtered correctly into account and privacy suite items`() =
        runTest {
            monitorConnectivityUseCase.stub {
                on { invoke() }.thenReturn(flowOf(true))
            }

            val accountItem = NavDrawerItem.Account(
                destination = TestDestination,
                icon = Icons.Default.Home,
                title = android.R.string.ok,
                subTitle = null,
                actionLabel = null
            )

            val privacySuiteItem = NavDrawerItem.PrivacySuite(
                destination = TestDestination,
                icon = Icons.Default.Settings,
                title = android.R.string.cancel,
                subTitle = android.R.string.copy,
                link = "https://mega.app",
                appPackage = null
            )

            val menuItems = mapOf(
                1 to accountItem,
                2 to privacySuiteItem
            )

            initUnderTest(menuItems = menuItems)

            val state = underTest.uiState.value
            assertThat(state.myAccountItems).hasSize(1)
            assertThat(state.privacySuiteItems).hasSize(1)
            assertThat(state.myAccountItems[1]?.title).isEqualTo(android.R.string.ok)
            assertThat(state.privacySuiteItems[2]?.title).isEqualTo(android.R.string.cancel)
        }

    @ParameterizedTest(name = "isConnected: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that connectivity status is monitored and updates state`(isConnected: Boolean) =
        runTest {
            stubDefaultDependencies()
            monitorConnectivityUseCase.stub {
                on { invoke() }.thenReturn(flowOf(isConnected))
            }

            initUnderTest()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isConnectedToNetwork).isEqualTo(isConnected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that avatar color is fetched and updates state`() = runTest {
        stubDefaultDependencies()
        val expectedColor = -16711936 // Green color value
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.thenReturn(expectedColor)
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.avatarColor).isEqualTo(Color(expectedColor))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that avatar color fetch failure is handled gracefully`() = runTest {
        stubDefaultDependencies()
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.thenThrow(RuntimeException("Color fetch failed"))
        }

        initUnderTest()

        // Should not crash and state should remain with default values
        val state = underTest.uiState.value
        assertThat(state.avatarColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `test that avatar file is monitored and updates state`() = runTest {
        stubDefaultDependencies()
        val avatarFile = File("/path/to/avatar.jpg")

        monitorMyAvatarFile.stub {
            on { invoke() }.thenReturn(flowOf(avatarFile))
        }

        getMyAvatarFileUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(avatarFile)
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.avatar).isEqualTo(avatarFile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that account detail monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies()
        monitorAccountDetailUseCase.stub {
            on { invoke() }.thenReturn(flow { throw RuntimeException("Account detail error") })
        }

        initUnderTest()

        // Should not crash despite error in account detail monitoring
        val state = underTest.uiState.value
        assertThat(state).isNotNull()
    }

    @Test
    fun `test that avatar file monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies()
        monitorMyAvatarFile.stub {
            on { invoke() }.thenReturn(flow { throw RuntimeException("Avatar file error") })
        }

        initUnderTest()

        // Should not crash despite error in avatar file monitoring
        val state = underTest.uiState.value
        assertThat(state.avatar).isNull()
    }

    @Test
    fun `test that account details are processed correctly and update subtitle flows`() = runTest {
        stubDefaultDependencies()

        val accountDetail = createAccountDetail(
            usedStorage = 2000000L,
            totalStorage = 4000000L,
            usedRubbish = 2000000L,
            accountType = AccountType.PRO_I
        )

        monitorAccountDetailUseCase.stub {
            on { invoke() }.thenReturn(flowOf(accountDetail))
        }

        val mockStorageString = "2 MB"
        val mockTotalStorageString = "4 MB"
        val mockRubbishString = "2 MB"
        val mockAccountTypeName = android.R.string.ok

        fileSizeStringMapper.stub {
            on { invoke(2000000L) }.thenReturn(mockStorageString)
            on { invoke(4000000L) }.thenReturn(mockTotalStorageString)
            on { invoke(2000000L) }.thenReturn(mockRubbishString)
        }

        accountNameMapper.stub {
            on { invoke(AccountType.PRO_I) }.thenReturn(mockAccountTypeName)
        }

        getStringFromStringResMapper.stub {
            on { invoke(mockAccountTypeName) }.thenReturn("Pro I")
        }

        val menuItems = mapOf(
            10 to CurrentPlanItem,
            20 to StorageItem,
            90 to RubbishBinItem
        )

        initUnderTest(menuItems = menuItems)

        underTest.uiState.test {
            val state = awaitItem()

            assertThat(state.myAccountItems).hasSize(3)
            assertThat(state.myAccountItems[10]).isNotNull()
            assertThat(state.myAccountItems[20]).isNotNull()
            assertThat(state.myAccountItems[90]).isNotNull()

            val currentPlanItem = state.myAccountItems[10]
            val storageItem = state.myAccountItems[20]
            val rubbishBinItem = state.myAccountItems[90]

            currentPlanItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo("Pro I")
            }

            storageItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo("$mockStorageString/$mockTotalStorageString")
            }

            rubbishBinItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo(mockRubbishString)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `test that account details with null level detail default to FREE account type`() =
        runTest {
            stubDefaultDependencies()

            val accountDetail = createAccountDetail(
                usedStorage = 100L,
                totalStorage = 200L,
                usedRubbish = 50L,
                levelDetail = null
            )

            monitorAccountDetailUseCase.stub {
                on { invoke() }.thenReturn(flowOf(accountDetail))
            }

            val mockStorageString = "100B"
            val mockTotalStorageString = "200B"
            val mockRubbishString = "50B"
            val mockAccountTypeName = android.R.string.copy

            fileSizeStringMapper.stub {
                on { invoke(100L) }.thenReturn(mockStorageString)
                on { invoke(200L) }.thenReturn(mockTotalStorageString)
                on { invoke(50L) }.thenReturn(mockRubbishString)
            }

            accountNameMapper.stub {
                on { invoke(AccountType.FREE) }.thenReturn(mockAccountTypeName)
            }

            getStringFromStringResMapper.stub {
                on { invoke(mockAccountTypeName) }.thenReturn("Free")
            }

            val menuItems = mapOf(
                10 to CurrentPlanItem,
                20 to StorageItem,
                90 to RubbishBinItem
            )

            initUnderTest(menuItems = menuItems)

            underTest.uiState.test {
                val state = awaitItem()

                verify(fileSizeStringMapper).invoke(100L)
                verify(fileSizeStringMapper).invoke(200L)
                verify(fileSizeStringMapper).invoke(50L)
                verify(accountNameMapper).invoke(AccountType.FREE)
                verify(getStringFromStringResMapper).invoke(mockAccountTypeName)

                assertThat(state.myAccountItems).hasSize(3)

                state.myAccountItems[10]?.subTitle?.test {
                    assertThat(awaitItem()).isEqualTo("Free")
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that account details with zero storage values are handled correctly`() = runTest {
        stubDefaultDependencies()

        val accountDetail = createAccountDetail(
            usedStorage = 0L,
            totalStorage = 1000000000L,
            usedRubbish = 0L,
            accountType = AccountType.FREE
        )

        monitorAccountDetailUseCase.stub {
            on { invoke() }.thenReturn(flowOf(accountDetail))
        }

        val mockZeroSizeString = "0 B"
        val mock1GBString = "1 GB"
        val mockAccountTypeName = android.R.string.ok

        fileSizeStringMapper.stub {
            on { invoke(0L) }.thenReturn(mockZeroSizeString)
            on { invoke(1000000000L) }.thenReturn(mock1GBString)
        }

        accountNameMapper.stub {
            on { invoke(AccountType.FREE) }.thenReturn(mockAccountTypeName)
        }

        getStringFromStringResMapper.stub {
            on { invoke(mockAccountTypeName) }.thenReturn("Free")
        }

        val menuItems = mapOf(
            10 to CurrentPlanItem,
            20 to StorageItem,
            90 to RubbishBinItem
        )

        initUnderTest(menuItems = menuItems)

        underTest.uiState.test {
            val state = awaitItem()

            val currentPlanItem = state.myAccountItems[10]
            val storageItem = state.myAccountItems[20]
            val rubbishBinItem = state.myAccountItems[90]

            verify(fileSizeStringMapper, times(2)).invoke(0L)
            verify(accountNameMapper).invoke(AccountType.FREE)
            verify(getStringFromStringResMapper).invoke(mockAccountTypeName)

            assertThat(state.myAccountItems).hasSize(3)

            currentPlanItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo("Free")
            }

            storageItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo("$mockZeroSizeString/$mock1GBString")
            }

            rubbishBinItem?.subTitle?.test {
                assertThat(awaitItem()).isEqualTo(mockZeroSizeString)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createAccountDetail(
        usedStorage: Long = 0L,
        totalStorage: Long = 0L,
        usedRubbish: Long = 0L,
        accountType: AccountType = AccountType.FREE,
        storageDetail: AccountStorageDetail? = AccountStorageDetail(
            usedCloudDrive = 0L,
            usedRubbish = usedRubbish,
            usedIncoming = 0L,
            totalStorage = totalStorage,
            usedStorage = usedStorage,
            subscriptionMethodId = 0
        ),
        levelDetail: AccountLevelDetail? = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
            proExpirationTime = 0L,
            accountPlanDetail = null,
            accountSubscriptionDetailList = emptyList()
        ),
    ): AccountDetail {
        return AccountDetail(
            storageDetail = storageDetail,
            levelDetail = levelDetail,
            sessionDetail = null,
            transferDetail = null
        )
    }

    private suspend fun stubDefaultDependencies() {
        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(flowOf(false))
        }

        monitorMyAvatarFile.stub {
            on { invoke() }.thenReturn(flowOf(null))
        }

        whenever(getMyAvatarFileUseCase(false)).thenReturn(null)

        monitorAccountDetailUseCase.stub {
            on { invoke() }.thenReturn(flow { awaitCancellation() })
        }

        whenever(getMyAvatarColorUseCase()).thenReturn(0)

        whenever(getUserFullNameUseCase(forceRefresh = false)).thenReturn("Test User")
        whenever(getCurrentUserEmail()).thenReturn("test@example.com")

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flow { awaitCancellation() })
        }

        whenever(isAchievementsEnabledUseCase()).thenReturn(true)
    }

    private fun initUnderTest(
        menuItems: Map<Int, NavDrawerItem> = emptyMap(),
    ) {
        underTest = MenuViewModel(
            menuItems = menuItems,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            accountNameMapper = accountNameMapper,
            getStringFromStringResMapper = getStringFromStringResMapper,
            fileSizeStringMapper = fileSizeStringMapper,
            getUserFullNameUseCase = getUserFullNameUseCase,
            getCurrentUserEmail = getCurrentUserEmail,
            monitorUserUpdates = monitorUserUpdates,
            isAchievementsEnabledUseCase = isAchievementsEnabledUseCase,
            checkPasswordReminderUseCase = checkPasswordReminderUseCase,
            ioDispatcher = ioDispatcher,
        )
    }

    @Test
    fun `test that getUserFullNameUseCase is called and updates state correctly`() = runTest {
        stubDefaultDependencies()
        val expectedName = "John Doe"

        whenever(getUserFullNameUseCase(forceRefresh = false)).thenReturn(expectedName)

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.name).isEqualTo(expectedName)
            verify(getUserFullNameUseCase).invoke(forceRefresh = false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getCurrentUserEmail is called and updates state correctly`() = runTest {
        stubDefaultDependencies()
        val expectedEmail = "user@example.com"

        whenever(getCurrentUserEmail()).thenReturn(expectedEmail)

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.email).isEqualTo(expectedEmail)
            verify(getCurrentUserEmail).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorUserChanges responds to firstname changes`() = runTest {
        stubDefaultDependencies()
        val updatedName = "Jane Smith"
        val avatarFile = File("/path/to/avatar.jpg")

        whenever(getUserFullNameUseCase(forceRefresh = true)).thenReturn(updatedName)
        whenever(getMyAvatarFileUseCase(isForceRefresh = true)).thenReturn(avatarFile)

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flowOf(UserChanges.Firstname))
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.name).isEqualTo(updatedName)
            verify(getUserFullNameUseCase).invoke(forceRefresh = true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorUserChanges responds to lastname changes`() = runTest {
        stubDefaultDependencies()
        val updatedName = "John Doe Jr."
        val avatarFile = File("/path/to/new_avatar.jpg")

        whenever(getUserFullNameUseCase(forceRefresh = true)).thenReturn(updatedName)
        whenever(getMyAvatarFileUseCase(isForceRefresh = true)).thenReturn(avatarFile)

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flowOf(UserChanges.Lastname))
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.name).isEqualTo(updatedName)
            verify(getUserFullNameUseCase).invoke(forceRefresh = true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorUserChanges responds to email changes`() = runTest {
        stubDefaultDependencies()
        val updatedEmail = "newemail@example.com"

        whenever(getCurrentUserEmail()).thenReturn(updatedEmail)

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flowOf(UserChanges.Email))
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.email).isEqualTo(updatedEmail)
            verify(getCurrentUserEmail, times(2)).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorUserChanges ignores other user changes`() = runTest {
        stubDefaultDependencies()

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flowOf(UserChanges.Avatar))
        }

        initUnderTest()

        // Should not trigger refreshUserName or refreshCurrentUserEmail for Avatar changes
        verify(
            getUserFullNameUseCase,
            times(1)
        ).invoke(forceRefresh = false) // Only initial call
        verify(getCurrentUserEmail, times(1)).invoke() // Only initial call
    }

    @Test
    fun `test that monitorUserChanges handles exceptions gracefully`() = runTest {
        stubDefaultDependencies()

        monitorUserUpdates.stub {
            on { invoke() }.thenReturn(flow { throw RuntimeException("User updates error") })
        }

        initUnderTest()

        // Should not crash despite error in user updates monitoring
        val state = underTest.uiState.value
        assertThat(state).isNotNull()
        verify(getUserFullNameUseCase, times(0)).invoke(true)
    }

    @Test
    fun `test that getUserFullNameUseCase handles exceptions gracefully`() = runTest {
        stubDefaultDependencies()

        whenever(getUserFullNameUseCase(forceRefresh = false)).thenThrow(RuntimeException("Get full name error"))

        initUnderTest()

        val state = underTest.uiState.value
        assertThat(state.name).isNull()
    }

    @Test
    fun `test that getCurrentUserEmail handles exceptions gracefully`() = runTest {
        stubDefaultDependencies()

        whenever(getCurrentUserEmail()).thenThrow(RuntimeException("Get email error"))

        initUnderTest()

        // Should not crash despite error in getting email
        val state = underTest.uiState.value
        assertThat(state.email).isNull()
    }

    @Test
    fun `test that achievements item is included when achievements are enabled`() = runTest {
        stubDefaultDependencies()
        whenever(isAchievementsEnabledUseCase()).thenReturn(true)

        val menuItems = mapOf(
            10 to CurrentPlanItem,
            20 to StorageItem,
            40 to AchievementsItem,
            90 to RubbishBinItem
        )

        initUnderTest(menuItems = menuItems)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.myAccountItems).hasSize(4)
            assertThat(state.myAccountItems[10]).isNotNull()
            assertThat(state.myAccountItems[20]).isNotNull()
            assertThat(state.myAccountItems[40]).isNotNull() // AchievementsItem should be included
            assertThat(state.myAccountItems[90]).isNotNull()
            verify(isAchievementsEnabledUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that achievements item is filtered out when achievements are disabled`() = runTest {
        stubDefaultDependencies()
        whenever(isAchievementsEnabledUseCase()).thenReturn(false)

        val menuItems = mapOf(
            10 to CurrentPlanItem,
            20 to StorageItem,
            40 to AchievementsItem,
            90 to RubbishBinItem
        )

        initUnderTest(menuItems = menuItems)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.myAccountItems).hasSize(3)
            assertThat(state.myAccountItems[10]).isNotNull()
            assertThat(state.myAccountItems[20]).isNotNull()
            assertThat(state.myAccountItems[40]).isNull() // AchievementsItem should be filtered out
            assertThat(state.myAccountItems[90]).isNotNull()
            verify(isAchievementsEnabledUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that achievements item is not included when IsAchievementsEnabled throws exception`() =
        runTest {
            stubDefaultDependencies()
            whenever(isAchievementsEnabledUseCase()).thenThrow(RuntimeException("Achievements check failed"))

            val menuItems = mapOf(
                10 to CurrentPlanItem,
                20 to StorageItem,
                40 to AchievementsItem,
                90 to RubbishBinItem
            )

            initUnderTest(menuItems = menuItems)

            underTest.uiState.test {
                val state = awaitItem()
                // Should default to enabled (true) when exception occurs
                assertThat(state.myAccountItems).hasSize(3)
                assertThat(state.myAccountItems[10]).isNotNull()
                assertThat(state.myAccountItems[20]).isNotNull()
                assertThat(state.myAccountItems[40]).isNull() // AchievementsItem should not be included
                assertThat(state.myAccountItems[90]).isNotNull()
                verify(isAchievementsEnabledUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that achievements item is not included when IsAchievementsEnabled returns null`() =
        runTest {
            stubDefaultDependencies()
            whenever(isAchievementsEnabledUseCase()).thenReturn(null)

            val menuItems = mapOf(
                10 to CurrentPlanItem,
                20 to StorageItem,
                40 to AchievementsItem,
                90 to RubbishBinItem
            )

            initUnderTest(menuItems = menuItems)

            underTest.uiState.test {
                val state = awaitItem()
                // Should default to enabled (false) when null is returned
                assertThat(state.myAccountItems).hasSize(3)
                assertThat(state.myAccountItems[10]).isNotNull()
                assertThat(state.myAccountItems[20]).isNotNull()
                assertThat(state.myAccountItems[40]).isNull() // AchievementsItem should not be included
                assertThat(state.myAccountItems[90]).isNotNull()
                verify(isAchievementsEnabledUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that achievements filtering works with only achievements item`() = runTest {
        stubDefaultDependencies()
        whenever(isAchievementsEnabledUseCase()).thenReturn(false)

        val menuItems = mapOf(
            40 to AchievementsItem
        )

        initUnderTest(menuItems = menuItems)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.myAccountItems).isEmpty() // AchievementsItem should be filtered out
            verify(isAchievementsEnabledUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that achievements filtering works with achievements enabled and only achievements item`() =
        runTest {
            stubDefaultDependencies()
            whenever(isAchievementsEnabledUseCase()).thenReturn(true)

            val menuItems = mapOf(
                40 to AchievementsItem
            )

            initUnderTest(menuItems = menuItems)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.myAccountItems).hasSize(1)
                assertThat(state.myAccountItems[40]).isNotNull() // AchievementsItem should be included
                verify(isAchievementsEnabledUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that logout triggers test password screen event when password reminder is required`() = runTest {
        stubDefaultDependencies()
        whenever(checkPasswordReminderUseCase(true)).thenReturn(true)
        initUnderTest()

        underTest.logout()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(triggered)
            assertThat(state.showLogoutConfirmationEvent).isEqualTo(consumed)
            verify(checkPasswordReminderUseCase).invoke(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that logout triggers logout confirmation event when password reminder is not required`() = runTest {
        stubDefaultDependencies()
        whenever(checkPasswordReminderUseCase(true)).thenReturn(false)
        initUnderTest()

        underTest.logout()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(consumed)
            assertThat(state.showLogoutConfirmationEvent).isEqualTo(triggered)
            verify(checkPasswordReminderUseCase).invoke(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that logout handles password reminder check failure gracefully`() = runTest {
        stubDefaultDependencies()
        whenever(checkPasswordReminderUseCase(true)).thenThrow(RuntimeException("Password check failed"))
        initUnderTest()

        underTest.logout()

        underTest.uiState.test {
            val state = awaitItem()
            // Should not trigger any events on failure
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(consumed)
            assertThat(state.showLogoutConfirmationEvent).isEqualTo(consumed)
            verify(checkPasswordReminderUseCase).invoke(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetTestPasswordScreenEvent updates state correctly`() = runTest {
        stubDefaultDependencies()
        initUnderTest()

        // First, trigger the test password screen event
        whenever(checkPasswordReminderUseCase(true)).thenReturn(true)
        underTest.logout()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(triggered)
            cancelAndIgnoreRemainingEvents()
        }

        // Now reset the event
        underTest.resetTestPasswordScreenEvent()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetTestPasswordScreenEvent works when event is already consumed`() = runTest {
        stubDefaultDependencies()
        initUnderTest()

        // Reset the event when it's already consumed
        underTest.resetTestPasswordScreenEvent()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that initial state has correct default event values`() = runTest {
        stubDefaultDependencies()
        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showTestPasswordScreenEvent).isEqualTo(consumed)
            assertThat(state.showLogoutConfirmationEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }
} 