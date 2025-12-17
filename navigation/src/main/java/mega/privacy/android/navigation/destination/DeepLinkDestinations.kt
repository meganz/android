package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class DeepLinksDialogNavKey(
    val deepLink: String,
) : NoSessionNavKey.Optional, DialogNavKey

@Serializable
data class DeepLinksAfterFetchNodesDialogNavKey(
    val deepLink: String,
    val regexPatternType: RegexPatternType,
) : DialogNavKey