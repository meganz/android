package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetMeetingPermissionsEducationShownUseCaseTest {
    private val permissionRepository: PermissionRepository = mock()

    private val underTest = SetMeetingPermissionsEducationShownUseCase(permissionRepository)

    @Test
    fun `test that invoke marks the education as shown in the repository`() = runTest {
        underTest()

        verify(permissionRepository).setMeetingPermissionsEducationShown()
    }
}
