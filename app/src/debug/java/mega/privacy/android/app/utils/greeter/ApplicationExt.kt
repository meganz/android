package mega.privacy.android.app.utils.greeter

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

fun Application.observeComponentCreation(callback: AndroidComponentCreation) {
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
        private val fragmentSet: MutableSet<Fragment> = mutableSetOf()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            callback.onActivityCreated(activity)
            registerFragmentLifecycleCallbacks(activity)
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}

        private fun registerFragmentLifecycleCallbacks(activity: Activity) {
            if (activity !is FragmentActivity) return

            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                        super.onFragmentResumed(fm, fragment)

                        if (fragment in fragmentSet) return
                        fragmentSet.add(fragment)

                        when (fragment) {
                            is BottomSheetDialogFragment -> {
                                callback.onBottomSheetDialogFragmentCreated(fragment)
                            }
                            is DialogFragment -> {
                                callback.onDialogFragmentCreated(fragment)
                            }
                            else -> {
                                callback.onFragmentCreated(fragment)
                            }
                        }
                    }

                    override fun onFragmentDestroyed(fm: FragmentManager, fragment: Fragment) {
                        super.onFragmentDestroyed(fm, fragment)
                        fragmentSet.remove(fragment)
                    }
                },
                true
            )
        }
    })
}
