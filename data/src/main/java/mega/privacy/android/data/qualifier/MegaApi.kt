package mega.privacy.android.data.qualifier

import javax.inject.Qualifier

/** Annotation for an normal MegaApiAndroid dependency. */
@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class MegaApi
