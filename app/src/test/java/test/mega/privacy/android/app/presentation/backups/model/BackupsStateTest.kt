package test.mega.privacy.android.app.presentation.backups.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.backups.model.BackupsState
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

/**
 * Test class for [BackupsState]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupsStateTest {

    private lateinit var underTest: BackupsState

    @ParameterizedTest(name = "when the current backups folder node id is {0} and root backups folder node id is {1}, then is user in root backups folder level is {2}")
    @ArgumentsSource(BackupsFolderLevelTestArgumentsSource::class)
    fun `test that the user is in the root backups folder level`(
        currentBackupsFolderNodeId: NodeId,
        rootBackupsFolderNodeId: NodeId,
        isUserInRootBackupsFolderLevel: Boolean,
    ) {
        underTest = BackupsState(
            currentBackupsFolderNodeId = currentBackupsFolderNodeId,
            rootBackupsFolderNodeId = rootBackupsFolderNodeId,
        )
        assertThat(underTest.isUserInRootBackupsFolderLevel).isEqualTo(
            isUserInRootBackupsFolderLevel
        )
    }

    /**
     * The implementation of the [ArgumentsProvider] to test if the User is in a specific Backups
     * Folder level or not
     */
    private class BackupsFolderLevelTestArgumentsSource : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> = Stream.of(
            Arguments.of(123456L, 123456L, true),
            Arguments.of(789012L, 123456L, false),
        )
    }
}