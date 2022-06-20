package test.mega.privacy.android.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.core.util.Preconditions
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.HiltTestActivity
import mega.privacy.android.app.R

internal const val testFragmentTag = "underTest"

/**
 * launchFragmentInContainer from the androidx.fragment:fragment-testing library
 * is NOT possible to use right now as it uses a hardcoded Activity under the hood
 * (i.e. [EmptyFragmentActivity]) which is not annotated with @AndroidEntryPoint.
 *
 * As a workaround, use this function that is equivalent. It requires you to add
 * [HiltTestActivity] in the debug folder and include it in the debug AndroidManifest.xml file
 * as can be found in this project.
 */
internal inline fun <reified T : Fragment> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    crossinline action: Fragment.() -> Unit = {}
): ActivityScenario<HiltTestActivity>? {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra(
        "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
        themeResId
    )

    return ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->
        val fragment: Fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader),
            T::class.java.name
        )
        fragment.arguments = fragmentArgs
        activity.supportFragmentManager
            .beginTransaction()
            .add(R.id.test_fragment, fragment, testFragmentTag)
            .commitNow()

        fragment.action()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Fragment> HiltTestActivity.testFragment(): T {
    return supportFragmentManager.findFragmentByTag(testFragmentTag) as T
}

internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes id: Int
) = onNodeWithText(fromId(id = id))

internal fun fromId(@StringRes id: Int) =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
