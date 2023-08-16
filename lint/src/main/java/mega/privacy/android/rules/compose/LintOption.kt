package mega.privacy.android.rules.compose

import com.android.tools.lint.client.api.Configuration

/**
 * A layer of indirection for implementations of option loaders without needing to extend from
 * Detector. This goes along with [OptionLoadingDetector].
 */
internal interface LintOption {
  fun load(configuration: Configuration)
}
