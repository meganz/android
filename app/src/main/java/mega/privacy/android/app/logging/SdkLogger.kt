package mega.privacy.android.app.logging

import javax.inject.Qualifier

/**
 * Sdk logger
 *
 */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class SdkLogger()
