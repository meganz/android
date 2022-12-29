package mega.privacy.android.app.utils.greeter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class DebugGreeter @Inject constructor(
    @ApplicationContext private val context: Context,
) : Greeter, AndroidComponentCreation {
    private val Activity.isExcluded: Boolean
        get() = javaClass.simpleName in listOf<String>()

    private val Fragment.isExcluded: Boolean
        get() = javaClass.simpleName in listOf(
            "SupportRequestManagerFragment",
            "NavHostFragment",
        )

    private val DialogFragment.isExcluded: Boolean
        get() = javaClass.simpleName in listOf<String>()

    private val BottomSheetDialogFragment.isExcluded: Boolean
        get() = javaClass.simpleName in listOf<String>()

    private val Any.fileName: String
        get() = javaClass.fileName

    private val Class<*>.fileName: String
        get() {
            val isKotlinFile = declaredAnnotations.any { it.annotationClass == Metadata::class }
            return simpleName + (".kt".takeIf { isKotlinFile } ?: ".java")
        }

    override fun initialize() {
        if (context !is Application) return
        context.observeComponentCreation(this)
    }

    override fun onActivityCreated(activity: Activity) {
        if (activity.isExcluded) return
        greet(message = "Activity: ${activity.fileName}")
    }

    override fun onFragmentCreated(fragment: Fragment) {
        if (fragment.isExcluded) return
        greet(message = "Fragment: ${fragment.fileName}")
    }

    override fun onDialogFragmentCreated(fragment: DialogFragment) {
        if (fragment.isExcluded) return
        greet(message = "DialogFragment: ${fragment.fileName}")
    }

    override fun onBottomSheetDialogFragmentCreated(fragment: BottomSheetDialogFragment) {
        if (fragment.isExcluded) return
        greet(message = "BottomSheetDialogFragment: ${fragment.fileName}")
    }

    private fun greet(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Timber.d(message)
    }
}
