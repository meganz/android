package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.navkey.Suppressable

@Serializable
data object CookieDialogNavKey : DialogNavKey, NoSessionNavKey.Optional, Suppressable

@Serializable
data object AdConsentDialogNavKey : DialogNavKey, NoSessionNavKey.Optional, Suppressable
