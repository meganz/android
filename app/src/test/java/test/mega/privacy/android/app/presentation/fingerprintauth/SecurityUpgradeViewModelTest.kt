package test.mega.privacy.android.app.presentation.fingerprintauth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeViewModel
import mega.privacy.android.domain.usecase.account.UpgradeSecurity
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SecurityUpgradeViewModelTest {

    private lateinit var underTest: SecurityUpgradeViewModel
    private val upgradeSecurity = mock<UpgradeSecurity>()
    private val setSecurityUpgradeInApp = mock<SetSecurityUpgradeInApp>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = SecurityUpgradeViewModel(upgradeSecurity, setSecurityUpgradeInApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that upgrade security is invoked`() = runTest {
        underTest.upgradeAccountSecurity()
        verify(upgradeSecurity).invoke()
    }
}
