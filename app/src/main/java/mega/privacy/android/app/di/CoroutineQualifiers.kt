package mega.privacy.android.app.di

import javax.inject.Qualifier

//Dispatcher qualifiers

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainImmediateDispatcher


//CoroutineScopeQualifiers

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope