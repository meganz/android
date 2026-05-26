package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.data.model.MegaPreferences
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaError
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Util {

    @JvmField
    val DATE_AND_TIME_PATTERN: String = "yyyy-MM-dd HH.mm.ss"

    @JvmField
    var dpWidthAbs: Float = 360f

    @JvmField
    var countryCodeDisplay: HashMap<String, String>? = null

    // 150ms, a smaller value may cause the keyboard to fail to open
    @JvmField
    val SHOW_IM_DELAY: Long = 150

    /**
     * Language tag for simplified Chinese.
     */
    private const val HANS = "Hans"

    /*
     * Build error dialog
     * @param message Message to display
     * @param finish Should activity finish after dialog dismis
     * @param activity Source activity
     */
    @JvmStatic
    fun showErrorAlertDialog(message: String?, finish: Boolean, activity: Activity?) {
        if (activity == null) {
            return
        }

        try {
            val dialogBuilder = getCustomAlertBuilder(
                activity,
                activity.getString(R.string.general_error_word),
                message,
                null
            )
            dialogBuilder.setPositiveButton(activity.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                if (finish) {
                    activity.finish()
                }
            }
            dialogBuilder.setOnCancelListener {
                if (finish) {
                    activity.finish()
                }
            }

            val dialog = dialogBuilder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()
            brandAlertDialog(dialog)
        } catch (ex: Exception) {
            showToast(activity, message)
        }
    }

    @JvmStatic
    fun showErrorAlertDialog(error: MegaError, activity: Activity?) {
        showErrorAlertDialog(error.errorString, false, activity)
    }

    @JvmStatic
    fun showErrorAlertDialog(errorCode: Int, activity: Activity?) {
        showErrorAlertDialog(MegaError.getErrorString(errorCode), false, activity)
    }

    @JvmStatic
    fun getCountryCodeByNetwork(context: Context): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (tm != null) {
            return tm.networkCountryIso
        }
        return null
    }

    /**
     * Indicates whether the device is currently roaming on this network
     *
     * @param context
     * @return Boolean. True if the device is currently roaming on this network otherwise false
     */
    @Deprecated(
        "Use mega.privacy.android.domain.usecase.environment.IsConnectivityInRoamingStateUseCase instead."
    )
    @JvmStatic
    fun isRoaming(context: Context): Boolean {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val ni = cm.activeNetworkInfo
            if (ni != null) {
                return ni.isRoaming
            }
        }
        return true
    }

    @JvmStatic
    fun countMatches(pattern: Pattern, string: String): Int {
        var count = 0
        var pos = 0
        try {
            val matcher = pattern.matcher(string)

            while (matcher.find(pos)) {
                count++
                pos = matcher.start() + 1
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return count
    }

    @JvmStatic
    fun String?.toCDATAOrNull() = this?.toCDATA() ?: this

    fun String.toCDATA() = this.apply {
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }.let { ChatUtil.converterShortCodes(it) } ?: this

    @JvmStatic
    fun getNumberItemChildren(file: File, context: Context): String {
        val list = file.listFiles()
        var count = 0
        if (list != null) {
            count = list.size
        }

        return context.resources.getQuantityString(R.plurals.general_num_items, count, count)
    }

    @JvmStatic
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth

        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /*
     * Build custom dialog
     * @param activity Source activity
     * @param title Dialog title
     * @param message To display, could be null
     * @param view Custom view to display in the dialog
     */
    @JvmStatic
    fun getCustomAlertBuilder(
        activity: Activity,
        title: String?,
        message: String?,
        view: View?,
    ): MaterialAlertDialogBuilder {
        val dialogBuilder = MaterialAlertDialogBuilder(activity)
        val customView = getCustomAlertView(activity, title, message)
        if (view != null) {
            customView.addView(view)
        }
        dialogBuilder.setView(customView)
        dialogBuilder.setInverseBackgroundForced(true)
        return dialogBuilder
    }

    /*
     * Create custom alert dialog view
     */
    private fun getCustomAlertView(activity: Activity, title: String?, message: String?): ViewGroup {
        val customView = activity.layoutInflater.inflate(R.layout.alert_dialog, null)

        val titleView = customView.findViewById<TextView>(R.id.dialog_title)
        titleView.text = title

        val messageView = customView.findViewById<TextView>(R.id.message)
        if (message == null) {
            messageView.visibility = View.GONE
        } else {
            messageView.text = message
        }
        return customView as ViewGroup
    }

    /*
     * Show Toast message with String
     */
    @JvmStatic
    fun showToast(context: Context?, message: String?) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
        }
    }

    @JvmStatic
    fun getScaleW(outMetrics: DisplayMetrics, density: Float): Float {
        var scale = 0f

        val dpWidth = outMetrics.widthPixels / density
        scale = dpWidth / dpWidthAbs

        return scale
    }

    /**
     * Convert dp to px.
     *
     * @param dp         dp value
     * @param outMetrics display metrics
     * @return corresponding dp value
     */
    @JvmStatic
    fun dp2px(dp: Float, outMetrics: DisplayMetrics?): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, outMetrics).toInt()
    }

    @JvmStatic
    fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    /*
     * AES encryption
     */
    @JvmStatic
    @Throws(Exception::class)
    fun aes_encrypt(raw: ByteArray, clear: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        val encrypted = cipher.doFinal(clear)
        return encrypted
    }

    /*
     * AES decryption
     */
    @JvmStatic
    @Throws(Exception::class)
    fun aes_decrypt(raw: ByteArray, encrypted: ByteArray): ByteArray {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        val decrypted = cipher.doFinal(encrypted)
        return decrypted
    }

    /**
     * Check if device connect to network
     */
    @Deprecated("use MonitorConnectivityUseCase instead")
    @JvmStatic
    fun isOnline(context: Context?): Boolean {
        if (context == null) return true

        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if (netInfo != null && netInfo.isConnectedOrConnecting) {
            return true
        }
        return false
    }

    /**
     * Gets a size string.
     * Matches iOS ByteCountFormatter behavior with .memory count style:
     * - KB values are rounded to whole numbers
     * - MB values show 1 decimal place
     * - GB and above show 2 decimal places
     *
     * @param size the size to show in the string
     * @return The size string.
     */
    @Deprecated("Use [mega.privacy.android.app.presentation.mapper.file.FileSizeMapper] instead")
    @JvmStatic
    fun getSizeString(size: Long, context: Context?): String {
        return formatFileSize(size, context!!)
    }

    @JvmStatic
    fun getSizeStringGBBased(gbSize: Long): String {
        val sizeString: String
        val decf = DecimalFormat("###.##")

        val TB = 1024f

        val context = MegaApplication.getInstance().applicationContext
        sizeString = if (gbSize < TB) {
            context.getString(
                mega.privacy.android.shared.resources.R.string.label_file_size_gigabytes,
                decf.format(gbSize)
            )
        } else {
            context.getString(
                mega.privacy.android.shared.resources.R.string.label_file_size_terabytes,
                decf.format((gbSize / TB).toDouble())
            )
        }

        return sizeString
    }

    @JvmStatic
    fun brandAlertDialog(dialog: AlertDialog) {
        try {
            val resources = dialog.context.resources

            val alertTitleId = resources.getIdentifier("alertTitle", "id", "android")

            val alertTitle = dialog.window?.decorView?.findViewById<TextView>(alertTitleId)
            if (alertTitle != null) {
                alertTitle.setTextColor(
                    ContextCompat.getColor(dialog.context, R.color.red_600_red_300)
                ) // change title text color
            }

            val titleDividerId = resources.getIdentifier("titleDivider", "id", "android")
            val titleDivider = dialog.window?.decorView?.findViewById<View>(titleDividerId)
            if (titleDivider != null) {
                titleDivider.setBackgroundColor(
                    ContextCompat.getColor(dialog.context, R.color.red_600_red_300)
                ) // change divider color
            }
        } catch (ex: Exception) {
            Toast.makeText(dialog.context, ex.message, Toast.LENGTH_LONG).show()
            ex.printStackTrace()
        }
    }

    /*
     * Get localized progress size
     */
    @JvmStatic
    fun getProgressSize(context: Context, progress: Long, size: Long): String {
        return String.format(
            "%s/%s",
            getSizeString(progress, context),
            getSizeString(size, context)
        )
    }

    /*
     * Set alpha transparency for view
     */
    @SuppressLint("NewApi")
    @JvmStatic
    fun setViewAlpha(view: View, alpha: Float) {
        view.alpha = alpha
    }

    @JvmStatic
    fun getPhotoSyncName(timeStamp: Long, fileName: String): String {
        val sdf: DateFormat = SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.getDefault())
        return sdf.format(Date(timeStamp)) + fileName.substring(fileName.lastIndexOf('.'))
    }

    @JvmStatic
    fun getLocalIpAddress(context: Context): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val interfaceName = intf.name

                // Ensure get the IP from the current active network interface
                val cm = context.applicationContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeInterfaceName = cm.getLinkProperties(cm.activeNetwork)?.interfaceName
                if (interfaceName.compareTo(activeInterfaceName ?: "") != 0) {
                    continue
                }

                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress != null && !inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex, "Error getting local IP address")
        }
        return null
    }

    /**
     * Returns the consumer friendly device name.
     * If Android version is above 7, the name is manufacturer + custom name set by user, otherwise, will be manufacturer + model.
     *
     * @return Device name, always starts with manufacturer, prefer user set name.
     */
    @JvmStatic
    fun getDeviceName(): String {
        return Build.MANUFACTURER + " " + Settings.Global.getString(
            MegaApplication.getInstance().contentResolver,
            Settings.Global.DEVICE_NAME
        )
    }

    @JvmStatic
    fun scaleHeightPx(px: Int, metrics: DisplayMetrics?): Int {
        val myHeightPx = metrics!!.heightPixels

        return px * myHeightPx / 548 //Based on Eduardo's measurements
    }

    @JvmStatic
    fun scaleWidthPx(px: Int, metrics: DisplayMetrics?): Int {
        val myWidthPx = metrics!!.widthPixels

        return px * myWidthPx / 360 //Based on Eduardo's measurements
    }

    @JvmStatic
    fun showAlert(context: Context, message: String?, title: String?): AlertDialog {
        Timber.d("showAlert")
        return showAlert(context, message, title, null)
    }

    /**
     * Show a simple alert dialog with a 'OK' button to dismiss itself.
     *
     * @param context  Context
     * @param message  the text content.
     * @param title    the title of the dialog, optional.
     * @param listener callback when press 'OK' button, optional.
     * @return the created alert dialog, the caller should cancel the dialog when the context destoried, otherwise window will leak.
     */
    @JvmStatic
    fun showAlert(
        context: Context,
        message: String?,
        title: String?,
        listener: DialogInterface.OnDismissListener?,
    ): AlertDialog {
        Timber.d("showAlert")
        val builder = AlertDialog.Builder(context)
        if (title != null) {
            builder.setTitle(title)
        }
        builder.setMessage(message)
        builder.setPositiveButton(
            context.getString(mega.privacy.android.shared.resources.R.string.general_ok),
            null
        )
        if (listener != null) {
            builder.setOnDismissListener(listener)
        }
        return builder.show()
    }

    @JvmStatic
    fun calculateTimestamp(time: String): Long {
        Timber.d("calculateTimestamp: %s", time)
        var unixtime: Long
        val dfm: DateFormat = SimpleDateFormat("yyyyMMddHHmm")
        dfm.timeZone = TimeZone.getDefault() //Specify your timezone
        try {
            unixtime = dfm.parse(time).time
            unixtime = unixtime / 1000
            return unixtime
        } catch (e: ParseException) {
            Timber.e(e)
        }
        return 0
    }

    @JvmStatic
    fun calculateDateFromTimestamp(timestamp: Long): Calendar {
        Timber.d("calculateTimestamp: %s", timestamp)
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp * 1000
        Timber.d("Calendar: %d %d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        return cal
    }

    @JvmStatic
    fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        bitmap.recycle()

        return output
    }

    //restrict the scale factor to below 1.1 to allow user to have some level of freedom and also prevent ui issues
    @JvmStatic
    fun setAppFontSize(activity: Activity) {
        val scale = activity.resources.configuration.fontScale
        Timber.d("System font size scale is %s", scale)

        val newScale: Float = if (scale <= 1.1) {
            scale
        } else {
            1.1f
        }

        Timber.d("New font size new scale is %s", newScale)
        val configuration = activity.resources.configuration
        configuration.fontScale = newScale

        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        metrics.scaledDensity = configuration.fontScale * metrics.density
        activity.baseContext.resources.updateConfiguration(configuration, metrics)
    }

    @JvmStatic
    fun mutateIconSecondary(context: Context, idDrawable: Int, idColor: Int): Drawable {
        var icon = ContextCompat.getDrawable(context, idDrawable)
        icon = icon!!.mutate()
        icon.setColorFilter(ContextCompat.getColor(context, idColor), PorterDuff.Mode.SRC_ATOP)

        return icon
    }

    /**
     * Gets the status bar height if available.
     *
     * @return The status bar height if available.
     */
    @JvmStatic
    fun getStatusBarHeight(): Int {
        return getSystemBarHeight("status_bar_height")
    }

    /**
     * Gets the navigation bar height if available.
     *
     * @return The status bar height if available.
     */
    @JvmStatic
    fun getNavigationBarHeight(): Int {
        return getSystemBarHeight("navigation_bar_height")
    }

    /**
     * Gets a system bar height if available.
     *
     * @param systemBarName The system bar name.
     * @return The system bar height if available.
     */
    @JvmStatic
    fun getSystemBarHeight(systemBarName: String): Int {
        val context = MegaApplication.getInstance().baseContext
        val resourceId = context.resources.getIdentifier(
            systemBarName, "dimen",
            "android"
        )

        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId)
        else 0
    }

    @JvmStatic
    fun getPreferences(): MegaPreferences? {
        return getDbHandler().preferences
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity?) {
        val imm = activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Method to display a simple Snackbar
     *
     *
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context Class where the Snackbar has to be shown
     * @param message Text to shown in the snackbar
     */
    @JvmStatic
    fun showSnackbar(context: Context, message: String?) {
        showSnackbar(context, Constants.SNACKBAR_TYPE, message, INVALID_HANDLE)
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     *
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context      Class where the Snackbar has to be shown
     * @param snackbarType specifies the type of the Snackbar.
     * It can be SNACKBAR_TYPE or MESSAGE_SNACKBAR_TYPE
     * @param message      Text to shown in the snackbar
     * @param idChat       Chat ID. If this param has a valid value, different to -1, the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat
     */
    @JvmStatic
    fun showSnackbar(context: Context, snackbarType: Int, message: String?, idChat: Long) {
        if (context is SnackbarShower) {
            context.showSnackbar(snackbarType, message, idChat)
        } else {
            Timber.w("Unable to show snack bar, view does not exist or context is not instance of SnackbarShower")
        }
    }

    @JvmStatic
    fun getRootViewFromContext(context: Context): View? {
        val activity = context as BaseActivity
        var rootView: View? = null
        try {
            rootView = activity.findViewById(android.R.id.content)
            if (rootView == null) {
                rootView = activity.window.decorView.findViewById(android.R.id.content)
            }
            if (rootView == null) {
                rootView = ((context as BaseActivity).findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) //get first view
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return rootView
    }

    @JvmStatic
    fun normalizePhoneNumber(phoneNumber: String, countryCode: String): String? {
        return PhoneNumberUtils.formatNumberToE164(phoneNumber, countryCode)
    }

    /**
     * Get normalized phone number by network
     *
     * @param context
     * @param phoneNumber
     * @return String. Normalized phone number
     */
    @Deprecated(
        "Use mega.privacy.android.domain.usecase.contact.GetNormalizedPhoneNumberByNetworkUseCase instead."
    )
    @JvmStatic
    fun normalizePhoneNumberByNetwork(context: Context, phoneNumber: String): String? {
        val countryCode = getCountryCodeByNetwork(context) ?: return null
        return normalizePhoneNumber(phoneNumber, countryCode.uppercase(Locale.getDefault()))
    }

    /**
     * This method formats the coordinates of a location in degrees, minutes and seconds
     * and returns a string with it
     *
     * @param latitude  latitude of the location to format
     * @param longitude longitude of the location to format
     * @return string with the location formatted in degrees, minutes and seconds
     */
    @Deprecated("Use ChatLocationMessageView.getGPSCoordinates instead.")
    @JvmStatic
    fun convertToDegrees(latitude: Float, longitude: Float): String {
        val builder = StringBuilder()

        formatCoordinate(builder, latitude)
        if (latitude < 0) {
            builder.append("S ")
        } else {
            builder.append("N ")
        }

        formatCoordinate(builder, longitude)
        if (longitude < 0) {
            builder.append("W")
        } else {
            builder.append("E")
        }

        return builder.toString()
    }

    /**
     * This method formats a coordinate in degrees, minutes and seconds
     *
     * @param builder    StringBuilder where the string formatted it's going to be built
     * @param coordinate coordinate to format
     */
    @Deprecated("Use ChatLocationMessageView.formatCoordinate instead.")
    private fun formatCoordinate(builder: StringBuilder, coordinate: Float) {
        val degrees = Location.convert(Math.abs(coordinate).toDouble(), Location.FORMAT_SECONDS)
        val degreesSplit = degrees.split(":".toRegex()).toTypedArray()
        builder.append(degreesSplit[0])
        builder.append("°")
        builder.append(degreesSplit[1])
        builder.append("'")

        try {
            builder.append(Math.round(degreesSplit[2].replace(",", ".").toFloat()))
        } catch (e: Exception) {
            Timber.w(e, "Error rounding seconds in coordinates")
            builder.append(degreesSplit[2])
        }

        builder.append("''")
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity?, flag: Int) {
        val v = activity!!.currentFocus
        if (v != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, flag)
        }
    }

    @JvmStatic
    fun hideKeyboardView(context: Context, v: View?, flag: Int) {
        if (v != null) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, flag)
        }
    }

    @JvmStatic
    fun isScreenInPortrait(context: Context?): Boolean {
        return context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    /**
     * This method detects whether the url matches certain URL regular expressions
     *
     * @param url    the passed url to be detected
     * @param regexs the array of URL regular expressions
     */
    @Deprecated("use mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase instead.")
    @JvmStatic
    fun matchRegexs(url: String?, regexs: Array<String>): Boolean {
        if (url == null) {
            return false
        }
        for (regex in regexs) {
            if (url.matches(regex.toRegex())) {
                Timber.d("REGEX MATCH: %s", regex)
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun showKeyboardDelayed(view: View?) {
        if (view == null) return

        val handler = Handler()
        handler.postDelayed({
            // The view needs to request the focus or the keyboard may not pops up
            if (view.requestFocus()) {
                val imm = MegaApplication.getInstance().applicationContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }, SHOW_IM_DELAY)
    }

    @JvmStatic
    fun checkTakePicture(activity: Activity, option: Int) {
        if (CallUtil.isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
            if (option == Constants.TAKE_PHOTO_CODE) {
                CallUtil.showConfirmationOpenCamera(activity, Constants.ACTION_TAKE_PICTURE, false)
            } else if (option == Constants.TAKE_PICTURE_PROFILE_CODE) {
                CallUtil.showConfirmationOpenCamera(
                    activity,
                    Constants.ACTION_TAKE_PROFILE_PICTURE,
                    false
                )
            }
            return
        }
        takePicture(activity, option)
    }

    /**
     * This method is to start camera from Activity
     *
     * @param activity the activity the camera would start from
     */
    @JvmStatic
    fun takePicture(activity: Activity, option: Int) {
        Timber.d("takePicture")
        val newFile = CacheFolderManager.buildTempFile("picture.jpg")
        try {
            newFile?.createNewFile()
        } catch (e: IOException) {
        }

        //This method is in the v4 support library, so can be applied to all devices
        val outputFileUri = FileProvider.getUriForFile(
            activity,
            Constants.AUTHORITY_STRING_FILE_PROVIDER,
            newFile!!
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        cameraIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        cameraIntent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            activity.startActivityForResult(cameraIntent, option)
        } catch (e: Exception) {
            Timber.d("Can not handle action MediaStore.ACTION_IMAGE_CAPTURE")
        }
    }

    @JvmStatic
    fun resetActionBar(aB: ActionBar?) {
        if (aB != null) {
            val customView = aB.customView
            if (customView != null) {
                val parent = customView.parent
                if (parent != null) {
                    (parent as ViewGroup).removeView(customView)
                }
            }
            aB.setDisplayShowCustomEnabled(false)
            aB.setDisplayShowTitleEnabled(true)
        }
    }

    /**
     * Checks if the current Android version is Android 11 or upper.
     *
     * @return True if the current Android version is Android 11 or upper, false otherwise.
     */
    @JvmStatic
    fun isAndroid11OrUpper(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    @JvmStatic
    fun isAndroid10OrUpper(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    @JvmStatic
    fun setPasswordToggle(textInputLayout: TextInputLayout?, focus: Boolean) {
        if (focus) {
            textInputLayout!!.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            textInputLayout.setEndIconDrawable(R.drawable.password_toggle)
        } else {
            textInputLayout!!.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    /**
     * Changes the elevation of the the ActionBar passed as parameter.
     *
     * @param aB            ActionBar in which the elevation has to be applied.
     * @param withElevation true if should apply elevation, false otherwise.
     * @param outMetrics    DisplayMetrics of the current device.
     */
    @JvmStatic
    fun changeViewElevation(aB: ActionBar?, withElevation: Boolean, outMetrics: DisplayMetrics?) {
        val elevation = dp2px(4f, outMetrics).toFloat()

        if (withElevation) {
            aB!!.elevation = elevation
        } else {
            aB!!.elevation = 0f
        }
    }

    /**
     * Gets a reference to a given drawable and prepares it for use with tinting through.
     *
     * @param resId the resource id for the given drawable
     * @return a wrapped drawable ready fo use
     * with [DrawableCompat]'s tinting methods
     * @throws Resources.NotFoundException
     */
    @JvmStatic
    @Throws(Resources.NotFoundException::class)
    fun getWrappedDrawable(context: Context, @DrawableRes resId: Int): Drawable {
        return DrawableCompat.wrap(
            ResourcesCompat.getDrawable(
                context.resources,
                resId, null
            )!!
        )
    }

    /**
     * Judge if current mode is Dark mode
     *
     * @param context the Context
     * @return true if it is dark mode, false for light mode
     */
    @JvmStatic
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Method for displaying a snack bar when is Offline.
     *
     * @return True, is is Offline. False it is Online.
     */
    @JvmStatic
    fun isOffline(context: Context?): Boolean {
        if (!isOnline(context)) {
            showSnackbar(context!!, context.getString(R.string.error_server_connection_problem))
            return true
        }
        return false
    }

    /**
     * Create a RecyclerView.ItemAnimator that doesn't support change animation.
     *
     * @return the RecyclerView.ItemAnimator
     */
    @JvmStatic
    fun noChangeRecyclerViewItemAnimator(): RecyclerView.ItemAnimator {
        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        return itemAnimator
    }

    /**
     * Judge if an activity is on the top of the running app task
     *
     * @param className the class name of the activity
     * @param context   the Context
     * @return true if the activity is on the task top, false otherwise
     */
    @JvmStatic
    fun isTopActivity(className: String?, context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val tasks = am.appTasks
        for (task in tasks) {
            val taskInfo = task.taskInfo
            if (taskInfo.id != -1) {  // Task is running
                return taskInfo.topActivity!!.className.contains(className!!)
            }
        }

        return false
    }

    @JvmStatic
    fun isSimplifiedChinese(): Boolean {
        return Locale.getDefault().toLanguageTag().contains(HANS)
    }

    /**
     * Method to know the current orientation of the device
     *
     * @return current orientation of the device
     */
    @JvmStatic
    fun getCurrentOrientation(): Int {
        return MegaApplication.getInstance().applicationContext.resources.configuration.orientation
    }
}
