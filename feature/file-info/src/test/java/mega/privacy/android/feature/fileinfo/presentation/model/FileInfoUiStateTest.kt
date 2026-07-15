package mega.privacy.android.feature.fileinfo.presentation.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FileInfoUiStateTest {

    @Test
    fun `test that mapCoordinates returns the coordinates only for the owner`() {
        val coordinates = Coordinates(latitude = 1.0, longitude = 2.0)

        assertThat(
            FileInfoUiState(coordinates = coordinates, accessPermission = AccessPermission.OWNER)
                .mapCoordinates
        ).isEqualTo(coordinates)

        assertThat(
            FileInfoUiState(coordinates = coordinates, accessPermission = AccessPermission.FULL)
                .mapCoordinates
        ).isNull()
    }

    @Test
    fun `test that mapCoordinates is null when there are no coordinates`() {
        assertThat(
            FileInfoUiState(coordinates = null, accessPermission = AccessPermission.OWNER)
                .mapCoordinates
        ).isNull()
    }

    @ParameterizedTest
    @EnumSource(value = AccessPermission::class, names = ["OWNER", "FULL"])
    fun `test that description and tags are editable for owner and full access`(
        permission: AccessPermission,
    ) {
        val state = FileInfoUiState(accessPermission = permission)

        assertThat(state.canEditDescription).isTrue()
        assertThat(state.canEditTags).isTrue()
    }

    @ParameterizedTest
    @EnumSource(value = AccessPermission::class, names = ["READ", "READWRITE", "UNKNOWN"])
    fun `test that description and tags are not editable for read or unknown access`(
        permission: AccessPermission,
    ) {
        val state = FileInfoUiState(accessPermission = permission)

        assertThat(state.canEditDescription).isFalse()
        assertThat(state.canEditTags).isFalse()
    }

    @Test
    fun `test that description and tags are not editable in the rubbish bin or backups`() {
        val rubbish =
            FileInfoUiState(accessPermission = AccessPermission.OWNER, isNodeInRubbish = true)
        val backups =
            FileInfoUiState(accessPermission = AccessPermission.OWNER, isNodeInBackups = true)

        assertThat(rubbish.canEditDescription).isFalse()
        assertThat(rubbish.canEditTags).isFalse()
        assertThat(backups.canEditDescription).isFalse()
        assertThat(backups.canEditTags).isFalse()
    }

    @ParameterizedTest
    @EnumSource(value = AccessPermission::class, names = ["READ", "READWRITE", "FULL", "OWNER"])
    fun `test that tags are shown for any accessible node`(permission: AccessPermission) {
        assertThat(FileInfoUiState(accessPermission = permission).canShowTags).isTrue()
    }

    @Test
    fun `test that tags are not shown for unknown access or in the rubbish bin or backups`() {
        assertThat(FileInfoUiState(accessPermission = AccessPermission.UNKNOWN).canShowTags).isFalse()
        assertThat(
            FileInfoUiState(
                accessPermission = AccessPermission.OWNER,
                isNodeInRubbish = true
            ).canShowTags
        ).isFalse()
        assertThat(
            FileInfoUiState(
                accessPermission = AccessPermission.OWNER,
                isNodeInBackups = true
            ).canShowTags
        ).isFalse()
    }

    @Test
    fun `test that a folder shared with contacts is an outgoing share`() {
        assertThat(
            FileInfoUiState(isFile = false, sharedContactCount = 3).isOutgoingShare
        ).isTrue()
    }

    @Test
    fun `test that a file is never an outgoing share even with shared contacts`() {
        assertThat(
            FileInfoUiState(isFile = true, sharedContactCount = 3).isOutgoingShare
        ).isFalse()
    }

    @Test
    fun `test that a folder with no shared contacts is not an outgoing share`() {
        assertThat(
            FileInfoUiState(isFile = false, sharedContactCount = 0).isOutgoingShare
        ).isFalse()
    }

    @Test
    fun `test that a node with an owner email is an incoming share`() {
        assertThat(
            FileInfoUiState(ownerEmail = "owner@mail.com").isIncomingShare
        ).isTrue()
    }

    @Test
    fun `test that a node without an owner email is not an incoming share`() {
        assertThat(FileInfoUiState(ownerEmail = null).isIncomingShare).isFalse()
    }

    @Test
    fun `test that a folder with versioned files shows folder versions`() {
        assertThat(
            FileInfoUiState(isFile = false, numberOfVersions = 91).showFolderVersions
        ).isTrue()
    }

    @Test
    fun `test that a folder without versions does not show folder versions`() {
        assertThat(
            FileInfoUiState(isFile = false, numberOfVersions = 0).showFolderVersions
        ).isFalse()
    }

    @Test
    fun `test that a file never shows folder versions`() {
        assertThat(
            FileInfoUiState(isFile = true, numberOfVersions = 91).showFolderVersions
        ).isFalse()
    }

    @Test
    fun `test that a file with versions shows file versions`() {
        assertThat(
            FileInfoUiState(isFile = true, versionCount = 2).showFileVersions
        ).isTrue()
    }

    @Test
    fun `test that a file without versions does not show file versions`() {
        assertThat(
            FileInfoUiState(isFile = true, versionCount = 0).showFileVersions
        ).isFalse()
    }

    @Test
    fun `test that a folder never shows file versions`() {
        assertThat(
            FileInfoUiState(isFile = false, versionCount = 2).showFileVersions
        ).isFalse()
    }
}
