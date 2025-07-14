package mega.privacy.android.app.menu.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.NavDrawerItem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
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
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()

    private object TestDestination

    @BeforeAll
    fun initialisation() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
            monitorUserCredentialsUseCase
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
                link = "https://mega.nz",
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
    fun `test that user credentials are monitored and update state`() = runTest {
        stubDefaultDependencies()
        val credentials = UserCredentials(
            email = "test@example.com",
            session = "session123",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(credentials))
        }

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.email).isEqualTo("test@example.com")
            assertThat(state.name).isEqualTo("John Doe")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that user credentials with null email are filtered out`() = runTest {
        stubDefaultDependencies()
        val credentialsWithNullEmail = UserCredentials(
            email = null,
            session = "session123",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(credentialsWithNullEmail))
        }

        initUnderTest()

        // The state should not be updated since email is null and filter should exclude it
        val initialState = underTest.uiState.value
        assertThat(initialState.email).isNull()
        assertThat(initialState.name).isNull()
    }

    @Test
    fun `test that avatar color is fetched and updates state`() = runTest {
        stubDefaultDependencies()
        val expectedColor = -16711936 // Green color value
        getMyAvatarColorUseCase.stub {
            onBlocking { invoke() }.thenReturn(expectedColor)
        }

        val credentials = UserCredentials(
            email = "test@example.com",
            session = "session123",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(credentials))
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

        val credentials = UserCredentials(
            email = "test@example.com",
            session = "session123",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(credentials))
        }

        monitorMyAvatarFile.stub {
            on { invoke() }.thenReturn(flowOf(avatarFile))
        }

        getMyAvatarFileUseCase.stub {
            onBlocking { invoke(false) }.thenReturn(avatarFile)
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
    fun `test that user credentials monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies()
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flow { throw RuntimeException("User credentials error") })
        }

        initUnderTest()

        // Should not crash despite error in user credentials monitoring
        val state = underTest.uiState.value
        assertThat(state.email).isNull()
        assertThat(state.name).isNull()
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

            verify(fileSizeStringMapper, org.mockito.kotlin.times(2)).invoke(0L)
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

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(flowOf(null))
        }

        monitorMyAvatarFile.stub {
            on { invoke() }.thenReturn(flowOf(null))
        }

        whenever(getMyAvatarFileUseCase(false)).thenReturn(null)

        monitorAccountDetailUseCase.stub {
            on { invoke() }.thenReturn(flow { awaitCancellation() })
        }

        whenever(getMyAvatarColorUseCase()).thenReturn(0)
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
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase
        )
    }
} 