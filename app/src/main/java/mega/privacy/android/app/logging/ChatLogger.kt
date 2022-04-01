package mega.privacy.android.app.logging

import javax.inject.Qualifier

/**
 * Chat logger
 *
 */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ChatLogger()
