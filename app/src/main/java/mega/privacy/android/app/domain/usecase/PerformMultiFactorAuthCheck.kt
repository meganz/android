package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaRequestListenerInterface

interface PerformMultiFactorAuthCheck {
    operator fun invoke(request: MegaRequestListenerInterface)
}