package mega.privacy.android.app.presentation.contact

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.app.domain.usecase.shares.GetOutShares
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileContactListViewModelTest {
    private val email = "email"
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val createShareKeyUseCase: CreateShareKeyUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()
    private val getOutShare: GetOutShares = mock()
    private val underTest = FileContactListViewModel(
        monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
        createShareKeyUseCase = createShareKeyUseCase,
        getNodeByIdUseCase = getNodeByIdUseCase,
        getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
        areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
        getOutShares = getOutShare
    )

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            createShareKeyUseCase,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase
        )
    }

    @ParameterizedTest(name = "when feature flag enabled is {0}, are all contacts verified is {1}, then show unverified contact banner is {2}")
    @MethodSource("provideParameters")
    fun `test that the unverified contact banner visibility is controlled`(
        featureEnabled: Boolean,
        areContactsVerified: Boolean,
        showUnverifiedContactBanner: Boolean,
    ) = runTest {
        whenever(getContactVerificationWarningUseCase()).thenReturn(
            featureEnabled
        )
        val node = mock<MegaNode> {
            on { handle }.thenReturn(123456L)
        }
        val share = mock<MegaShare> {
            on { it.user }.thenReturn(email)
        }
        whenever(getOutShare(NodeId(node.handle))).thenReturn(listOf(share))
        whenever(areCredentialsVerifiedUseCase(email)).thenReturn(areContactsVerified)
        underTest.getMegaShares(node)
        underTest.showNotVerifiedContactBanner.test {
            val showBanner = awaitItem()
            Truth.assertThat(showBanner).isEqualTo(showUnverifiedContactBanner)
        }
        underTest.megaShare.test {
            val shares = awaitItem()
            Truth.assertThat(shares).isEqualTo(listOf(share))
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, true, false),
        Arguments.of(true, true, false),
        Arguments.of(true, false, true),
    )
}