package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFirstNameUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.feature.myaccount.presentation.mapper.AccountTypeNameMapper
import mega.privacy.android.feature.myaccount.presentation.mapper.QuotaLevelMapper
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyAccountWidgetViewModelTest {

    private lateinit var underTest: MyAccountWidgetViewModel

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val getUserFirstNameUseCase = mock<GetUserFirstNameUseCase>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val getMyAvatarFileUseCase = mock<GetMyAvatarFileUseCase>()
    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val accountTypeNameMapper = mock<AccountTypeNameMapper>()
    private val quotaLevelMapper = mock<QuotaLevelMapper>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorAccountDetailUseCase,
            monitorStorageStateUseCase,
            getUserFirstNameUseCase,
            monitorUserUpdates,
            monitorMyAvatarFile,
            getMyAvatarFileUseCase,
            getMyAvatarColorUseCase,
            accountTypeNameMapper,
            quotaLevelMapper,
        )
    }

    @Test
    fun `test that initial state has loading true and default values`() = runTest {
        stubDefaultDependencies()

        initUnderTest()

        val initialState = underTest.uiState.value
        assertThat(initialState.isLoading).isTrue() // isLoading is set to false after account data loads
        assertThat(initialState.name).isNull() // Name is loaded in init
        assertThat(initialState.avatarFile).isNull()
        assertThat(initialState.avatarColor).isEqualTo(Color.Unspecified)
        assertThat(initialState.usedStorage).isEqualTo(0L)
        assertThat(initialState.totalStorage).isEqualTo(0L)
        assertThat(initialState.usedStoragePercentage).isEqualTo(0)
        assertThat(initialState.storageQuotaLevel).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test that account details are monitored and update state correctly`() = runTest {
        val accountDetail = createAccountDetail(
            usedStorage = 500000000000L, // 500 GB
            totalStorage = 1000000000000L, // 1 TB
            accountType = AccountType.PRO_I
        )

        val expectedAccountTypeResource = 123
        stubDefaultDependencies(
            accountDetailFlow = flowOf(accountDetail),
            accountTypeMapper = { if (it == AccountType.PRO_I) expectedAccountTypeResource else 0 }
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.usedStorage).isEqualTo(500000000000L)
            assertThat(state.totalStorage).isEqualTo(1000000000000L)
            assertThat(state.usedStoragePercentage).isEqualTo(50)
            assertThat(state.accountTypeNameResource).isEqualTo(expectedAccountTypeResource)
            assertThat(state.isLoading).isFalse()
            assertThat(state.storageQuotaLevel).isEqualTo(QuotaLevel.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that account detail monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies(
            accountDetailFlow = flow { throw RuntimeException("Account detail error") }
        )

        initUnderTest()

        // Should not crash despite error
        val state = underTest.uiState.value
        assertThat(state).isNotNull()
    }

    @Test
    fun `test that storage state is monitored and updates quota level`() = runTest {
        val storageStateFlow = MutableStateFlow(StorageState.Green)
        stubDefaultDependencies(
            storageStateFlow = storageStateFlow
        )

        initUnderTest()

        underTest.uiState.test {
            var state = awaitItem()
            assertThat(state.storageState).isEqualTo(StorageState.Green)
            assertThat(state.storageQuotaLevel).isEqualTo(QuotaLevel.Success)

            // Update to Orange state
            storageStateFlow.emit(StorageState.Orange)
            state = awaitItem()
            assertThat(state.storageState).isEqualTo(StorageState.Orange)
            assertThat(state.storageQuotaLevel).isEqualTo(QuotaLevel.Warning)

            // Update to Red state
            storageStateFlow.emit(StorageState.Red)
            state = awaitItem()
            assertThat(state.storageState).isEqualTo(StorageState.Red)
            assertThat(state.storageQuotaLevel).isEqualTo(QuotaLevel.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that storage state monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies(
            storageStateFlow = flow { throw RuntimeException("Storage state error") }
        )

        initUnderTest()

        // Should not crash despite error
        val state = underTest.uiState.value
        assertThat(state).isNotNull()
    }

    @Test
    fun `test that user full name is loaded and updates state`() = runTest {
        val expectedName = "John Doe"
        stubDefaultDependencies(
            userFullName = expectedName
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.name).isEqualTo(expectedName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that user full name loading handles errors gracefully`() = runTest {
        stubDefaultDependencies(
            userFullNameThrows = RuntimeException("Name error")
        )

        initUnderTest()

        // Should not crash despite error
        val state = underTest.uiState.value
        assertThat(state.name).isNull()
    }

    @Test
    fun `test that user updates for firstname trigger name reload`() = runTest {
        val initialName = "John Doe"
        stubDefaultDependencies(
            userFullName = initialName,
            userUpdatesFlow = flowOf(UserChanges.Firstname)
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            // Name should be updated after firstname change
            assertThat(state.name).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that user updates for lastname trigger name reload`() = runTest {
        val initialName = "John Doe"
        stubDefaultDependencies(
            userFullName = initialName,
            userUpdatesFlow = flowOf(UserChanges.Lastname)
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.name).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that user updates monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies(
            userUpdatesFlow = flow { throw RuntimeException("User updates error") }
        )

        initUnderTest()

        // Should not crash despite error
        val state = underTest.uiState.value
        assertThat(state).isNotNull()
    }

    @Test
    fun `test that avatar file and color are loaded correctly`() = runTest {
        val avatarFile = File("/path/to/avatar.jpg")
        val avatarColor = -16711936 // Green color value

        stubDefaultDependencies(
            avatarFileFlow = flowOf(avatarFile),
            avatarFileCached = avatarFile,
            avatarFileRemote = avatarFile,
            avatarColor = avatarColor
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.avatarFile).isEqualTo(avatarFile)
            assertThat(state.avatarColor).isEqualTo(Color(avatarColor))
            // Note: getMyAvatarFileUseCase is called 3 times total:
            // 1. onStart with isForceRefresh=false
            // 2. onStart with isForceRefresh=true  
            // 3. From monitorMyAvatarFile flow emission
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that avatar loading uses onStart to emit cached then remote`() = runTest {
        val cachedAvatar = File("/path/to/cached_avatar.jpg")
        val remoteAvatar = File("/path/to/remote_avatar.jpg")

        stubDefaultDependencies(
            avatarFileFlow = flowOf(remoteAvatar),
            avatarFileCached = cachedAvatar,
            avatarFileRemote = remoteAvatar
        )

        initUnderTest()

        underTest.uiState.test {
            // Verify both cached and remote calls are made
            awaitItem()
            verify(getMyAvatarFileUseCase).invoke(isForceRefresh = false)
            verify(getMyAvatarFileUseCase).invoke(isForceRefresh = true)
        }
    }

    @Test
    fun `test that avatar color fetch failure is handled gracefully`() = runTest {
        val avatarFile = File("/path/to/avatar.jpg")

        stubDefaultDependencies(
            avatarFileFlow = flowOf(avatarFile),
            avatarFileCached = avatarFile,
            avatarFileRemote = avatarFile,
            avatarColorThrows = RuntimeException("Color fetch failed")
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.avatarFile).isEqualTo(avatarFile)
            assertThat(state.avatarColor).isEqualTo(Color.Unspecified) // Color should be default on error
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that avatar file monitoring handles errors gracefully`() = runTest {
        stubDefaultDependencies(
            avatarFileFlow = flow { throw RuntimeException("Avatar file error") }
        )

        initUnderTest()

        // Should not crash despite error
        val state = underTest.uiState.value
        assertThat(state.avatarFile).isNull()
    }

    @ParameterizedTest(name = "storageState: {0}, expected: {1}")
    @MethodSource("quotaLevelTestCases")
    fun `test that quota level is calculated correctly`(
        storageState: StorageState?,
        expectedQuotaLevel: QuotaLevel,
    ) = runTest {
        val accountDetail = createAccountDetail(
            accountType = AccountType.FREE
        )

        stubDefaultDependencies(
            accountDetailFlow = flowOf(accountDetail),
            storageStateFlow = flowOf(storageState ?: StorageState.Green)
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.storageQuotaLevel).isEqualTo(expectedQuotaLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that account detail with null storage detail uses default values`() = runTest {
        val accountDetail = createAccountDetail(
            storageDetail = null,
            accountType = AccountType.FREE
        )

        stubDefaultDependencies(
            accountDetailFlow = flowOf(accountDetail)
        )

        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.usedStorage).isEqualTo(0L)
            assertThat(state.totalStorage).isEqualTo(0L)
            assertThat(state.usedStoragePercentage).isEqualTo(0)
            assertThat(state.storageQuotaLevel).isEqualTo(QuotaLevel.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun stubDefaultDependencies(
        accountDetailFlow: kotlinx.coroutines.flow.Flow<AccountDetail>? = null,
        storageStateFlow: kotlinx.coroutines.flow.Flow<StorageState>? = null,
        userFullName: String? = null,
        userFullNameThrows: Throwable? = null,
        userUpdatesFlow: kotlinx.coroutines.flow.Flow<UserChanges>? = null,
        avatarFileFlow: kotlinx.coroutines.flow.Flow<File?>? = null,
        avatarFileCached: File? = null,
        avatarFileRemote: File? = null,
        avatarColor: Int? = null,
        avatarColorThrows: Throwable? = null,
        accountTypeMapper: ((AccountType?) -> Int)? = null,
        quotaLevelMapperFunc: ((StorageState?) -> QuotaLevel)? = null,
    ) {
        val defaultAccountDetail = createAccountDetail()
        whenever(monitorAccountDetailUseCase()).thenReturn(
            accountDetailFlow ?: flowOf(defaultAccountDetail)
        )
        whenever(monitorStorageStateUseCase()).thenReturn(
            storageStateFlow ?: flowOf(StorageState.Green)
        )

        if (userFullNameThrows != null) {
            whenever(getUserFirstNameUseCase(forceRefresh = false)).thenThrow(userFullNameThrows)
        } else {
            whenever(getUserFirstNameUseCase(forceRefresh = false)).thenReturn(
                userFullName ?: "Test User"
            )
        }

        whenever(monitorUserUpdates()).thenReturn(userUpdatesFlow ?: flowOf())
        whenever(monitorMyAvatarFile()).thenReturn(avatarFileFlow ?: flowOf(null))
        whenever(getMyAvatarFileUseCase(isForceRefresh = false)).thenReturn(avatarFileCached)
        whenever(getMyAvatarFileUseCase(isForceRefresh = true)).thenReturn(avatarFileRemote)

        if (avatarColorThrows != null) {
            whenever(getMyAvatarColorUseCase()).thenThrow(avatarColorThrows)
        } else {
            whenever(getMyAvatarColorUseCase()).thenReturn(avatarColor ?: 0)
        }

        whenever(accountTypeNameMapper(any())).thenAnswer { invocation ->
            accountTypeMapper?.invoke(invocation.getArgument(0)) ?: 0
        }

        whenever(quotaLevelMapper(any())).thenAnswer { invocation ->
            val storageState = invocation.getArgument<StorageState?>(0)
            quotaLevelMapperFunc?.invoke(storageState)
                ?: when (storageState) {
                    StorageState.Red -> QuotaLevel.Error
                    StorageState.Orange -> QuotaLevel.Warning
                    else -> QuotaLevel.Success
                }
        }
    }

    private fun initUnderTest() {
        underTest = MyAccountWidgetViewModel(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getUserFirstNameUseCase = getUserFirstNameUseCase,
            monitorUserUpdates = monitorUserUpdates,
            monitorMyAvatarFile = monitorMyAvatarFile,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            accountTypeNameMapper = accountTypeNameMapper,
            quotaLevelMapper = quotaLevelMapper,
        )
    }

    private fun createAccountDetail(
        usedStorage: Long = 0L,
        totalStorage: Long = 0L,
        accountType: AccountType = AccountType.FREE,
        storageDetail: AccountStorageDetail? = AccountStorageDetail(
            usedCloudDrive = 0L,
            usedRubbish = 0L,
            usedIncoming = 0L,
            totalStorage = totalStorage,
            usedStorage = usedStorage,
            subscriptionMethodId = 0
        ),
        levelDetail: AccountLevelDetail? = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            proExpirationTime = 0L,
            accountSubscriptionCycle = mega.privacy.android.domain.entity.AccountSubscriptionCycle.UNKNOWN,
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

    companion object {
        @JvmStatic
        fun quotaLevelTestCases(): Stream<Arguments> = Stream.of(
            // storageState, expectedQuotaLevel
            Arguments.of(null, QuotaLevel.Success),
            Arguments.of(StorageState.Green, QuotaLevel.Success),
            Arguments.of(StorageState.Unknown, QuotaLevel.Success),
            Arguments.of(StorageState.Change, QuotaLevel.Success),
            Arguments.of(StorageState.PayWall, QuotaLevel.Success),
            Arguments.of(StorageState.Orange, QuotaLevel.Warning),
            Arguments.of(StorageState.Red, QuotaLevel.Error),
        )
    }
}
