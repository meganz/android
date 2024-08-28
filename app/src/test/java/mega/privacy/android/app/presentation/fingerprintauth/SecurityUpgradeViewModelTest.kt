package mega.privacy.android.app.presentation.fingerprintauth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.UpgradeSecurity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
class SecurityUpgradeViewModelTest {

    private lateinit var underTest: SecurityUpgradeViewModel
    private val upgradeSecurity = mock<UpgradeSecurity>()
    private val setSecurityUpgradeInApp = mock<SetSecurityUpgradeInApp>()

    @BeforeEach
    fun setUp() {
        underTest = SecurityUpgradeViewModel(upgradeSecurity, setSecurityUpgradeInApp)
    }

    @Test
    fun `test that upgrade security is invoked`() = runTest {
        underTest.upgradeAccountSecurity()
        verify(upgradeSecurity).invoke()
    }
}
