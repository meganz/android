package mega.privacy.android.app.presentation.settings.passcode.model

import kotlinx.collections.immutable.ImmutableList

data class PasscodeTimeoutUIState(
    val options: ImmutableList<TimeoutOption>,
    val currentOption: TimeoutOption?
)