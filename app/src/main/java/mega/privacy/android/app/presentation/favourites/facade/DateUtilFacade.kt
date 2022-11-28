package mega.privacy.android.app.presentation.favourites.facade

import mega.privacy.android.data.wrapper.DateUtilWrapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * The implementation of DateUtilWrapper
 */
class DateUtilFacade @Inject constructor() : DateUtilWrapper {

    override fun fromEpoch(seconds: Long): LocalDateTime = LocalDateTime.from(
        LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault())
    )

}