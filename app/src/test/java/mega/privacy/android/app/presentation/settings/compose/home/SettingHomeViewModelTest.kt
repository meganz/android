package mega.privacy.android.app.presentation.settings.compose.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.compose.home.mapper.MyAccountSettingStateMapper
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.settings.MoreSettingEntryPoint
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SettingHomeViewModelTest {
    private lateinit var underTest: SettingHomeViewModel

    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()

    @BeforeEach
    fun setup() {
        reset(
            getAccountDetailsUseCase
        )
    }

    private fun initUnderTest(
        featureEntryPoints: Set<FeatureSettingEntryPoint> = setOf(),
        moreEntryPoints: Set<MoreSettingEntryPoint> = setOf(),
    ) {
        underTest = SettingHomeViewModel(
            featureEntryPoints = featureEntryPoints,
            moreEntryPoints = moreEntryPoints,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            myAccountSettingStateMapper = MyAccountSettingStateMapper()
        )
    }


    @Test
    fun `test that an empty set of settings returns loading state`() = runTest {
        getAccountDetailsUseCase.stub {
            onBlocking { invoke(any()) }.doSuspendableAnswer { suspendCancellableCoroutine { } }
        }
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(
                SettingsHomeState.Loading(
                    featureEntryPoints = persistentListOf(),
                    moreEntryPoints = persistentListOf(),
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that data state is returned if user account info is returned`() = runTest {
        getAccountDetailsUseCase.stub {
            onBlocking { invoke(any()) } doReturn UserAccount(
                userId = UserId(123L),
                email = "email",
                fullName = "fullname",
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = null,
                accountTypeString = ""
            )
        }
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem()).isInstanceOf(SettingsHomeState.Data::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

}