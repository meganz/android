package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaRequestListenerInterface

interface FetchAutoAcceptQRLinks {
    suspend operator fun invoke(): Boolean
}