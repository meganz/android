package mega.privacy.android.domain.qualifier

import javax.inject.Qualifier

/**
 * Mutex serialising every mutation of the coupled audio playback-mode preferences (repeat mode,
 * continuous playback, and its auto-enabled record).
 *
 * [mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioRepeatModeUseCase] and
 * [mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioContinuousPlaybackEnabledUseCase]
 * each run a read-then-write sequence across the same keys, suspending at every DataStore
 * boundary, and the invariants they maintain only hold within a single invocation. Concurrent
 * invocations — rapid repeat-button taps, or a repeat tap racing the queue screen's
 * continuous-playback toggle — take this lock so their steps cannot interleave.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class AudioPlaybackModeMutex
