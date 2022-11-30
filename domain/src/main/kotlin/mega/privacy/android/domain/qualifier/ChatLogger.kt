package mega.privacy.android.domain.qualifier

import javax.inject.Qualifier

/**
 * Chat logger
 *
 */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ChatLogger()
