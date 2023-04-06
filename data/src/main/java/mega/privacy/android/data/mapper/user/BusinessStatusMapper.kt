package mega.privacy.android.data.mapper.user

import mega.privacy.android.domain.entity.user.BusinessStatus
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

internal class BusinessStatusMapper @Inject constructor() {
    operator fun invoke(status: Int) = when (status) {
        MegaApiJava.BUSINESS_STATUS_EXPIRED -> BusinessStatus.EXPIRED
        MegaApiJava.BUSINESS_STATUS_INACTIVE -> BusinessStatus.INACTIVE
        MegaApiJava.BUSINESS_STATUS_ACTIVE -> BusinessStatus.ACTIVE
        MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD -> BusinessStatus.GRACE_PERIOD
        else -> null
    }
}