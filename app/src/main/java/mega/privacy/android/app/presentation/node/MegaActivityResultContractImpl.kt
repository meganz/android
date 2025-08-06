package mega.privacy.android.app.presentation.node

import mega.privacy.android.app.activities.contract.HiddenNodeOnboardingActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.activities.contract.ShareFolderActivityContract
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.navigation.MegaActivityResultContract
import javax.inject.Inject

/**
 * Implementation of MegaActivityResultContract that provides all the necessary
 * ActivityResultContract instances for node-related operations.
 */
class MegaActivityResultContractImpl @Inject constructor() : MegaActivityResultContract {

    override val versionsFileActivityResultContract: VersionsFileActivityContract =
        VersionsFileActivityContract()

    override val selectFolderToMoveActivityResultContract: SelectFolderToMoveActivityContract =
        SelectFolderToMoveActivityContract()

    override val selectFolderToCopyActivityResultContract: SelectFolderToCopyActivityContract =
        SelectFolderToCopyActivityContract()

    override val shareFolderActivityResultContract: ShareFolderActivityContract =
        ShareFolderActivityContract()

    override val sendToChatActivityResultContract: SendToChatActivityContract =
        SendToChatActivityContract()

    override val hiddenNodeOnboardingActivityResultContract: HiddenNodeOnboardingActivityContract =
        HiddenNodeOnboardingActivityContract()
} 