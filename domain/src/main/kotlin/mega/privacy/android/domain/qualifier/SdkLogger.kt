package mega.privacy.android.domain.qualifier

import javax.inject.Qualifier

/**
 * Sdk logger
 *
 */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class SdkLogger()
