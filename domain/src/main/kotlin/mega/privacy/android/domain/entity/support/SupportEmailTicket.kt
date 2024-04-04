package mega.privacy.android.domain.entity.support

import java.io.File

/**
 * Support email ticket
 *
 * @property email
 * @property ticket
 * @property logs
 * @property subject
 */
data class SupportEmailTicket(
    val email: String,
    val ticket: String,
    val logs: File?,
    val subject: String,
)
