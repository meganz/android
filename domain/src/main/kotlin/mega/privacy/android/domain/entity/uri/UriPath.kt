package mega.privacy.android.domain.entity.uri

import kotlinx.serialization.Serializable

/**
 * A value class that encapsulates a string representation of an Android Uri.
 *
 * @property value the string representation of the Uri
 */

@JvmInline
@Serializable
value class UriPath(val value: String)