package mega.privacy.android.app.providers

import android.database.Cursor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mega.privacy.android.app.initializer.DependencyContainer
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineDocumentProviderRootFolderUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.lang.reflect.Field

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineDocumentProviderTest {

    private lateinit var underTest: OfflineDocumentProvider
    private val getOfflineDocumentProviderRootFolderUseCase: GetOfflineDocumentProviderRootFolderUseCase =
        mock()
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase = mock()
    private val monitorLogoutUseCase: MonitorLogoutUseCase = mock()
    private val mockedRootFolder: File = mock()
    private val mockedAccountCredentials = mock<UserCredentials>()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockDependencyContainer: DependencyContainer = mock()

    @BeforeEach
    fun setUp() {
        reset(
            getOfflineDocumentProviderRootFolderUseCase,
            getAccountCredentialsUseCase,
            monitorLogoutUseCase,
            mockedRootFolder,
            mockedAccountCredentials,
            mockDependencyContainer
        )

        whenever(mockDependencyContainer.applicationScope).thenReturn(testScope)
        whenever(mockDependencyContainer.getOfflineDocumentProviderRootFolderUseCase).thenReturn(
            getOfflineDocumentProviderRootFolderUseCase
        )
        whenever(mockDependencyContainer.monitorLogoutUseCase).thenReturn(monitorLogoutUseCase)
        whenever(mockDependencyContainer.getAccountCredentialsUseCase).thenReturn(
            getAccountCredentialsUseCase
        )

        underTest = OfflineDocumentProvider()
        injectDependencyContainer(underTest, mockDependencyContainer)
    }

    private fun injectDependencyContainer(instance: Any, value: Any) {
        val field: Field = instance.javaClass.getDeclaredField("dependencyContainer")
        field.isAccessible = true
        field.set(instance, value)
    }

    @Test
    fun `test queryRoots returns empty cursor when user is not logged in`() = runBlocking {
        whenever(monitorLogoutUseCase()).thenReturn(flowOf(false))
        whenever(getAccountCredentialsUseCase()).thenReturn(null)
        val cursor: Cursor = underTest.queryRoots(null)
        assertThat(cursor.count).isEqualTo(0)
    }
}
