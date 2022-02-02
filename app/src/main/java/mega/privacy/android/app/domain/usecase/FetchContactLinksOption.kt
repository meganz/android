package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaRequestListenerInterface

interface FetchContactLinksOption {
    operator fun invoke(request: MegaRequestListenerInterface)
}