package mega.privacy.android.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Pair
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.vdurmont.emoji.EmojiParser
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR
import mega.privacy.android.app.utils.Constants.AVATAR_PHONE_COLOR
import mega.privacy.android.app.utils.Constants.AVATAR_PRIMARY_COLOR
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.Constants.DEFAULT_AVATAR_WIDTH_HEIGHT
import mega.privacy.android.app.utils.Constants.UNKNOWN_USER_NAME_AVATAR
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.ThumbnailUtils.getRoundedRect
import mega.privacy.android.app.utils.Util.calculateInSampleSize
import mega.privacy.android.app.utils.Util.getCircleBitmap
import mega.privacy.android.thirdpartylib.twemoji.EmojiManager
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtils
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.util.Locale

/**
 * Avatar util
 *
 * Legacy static util class
 */
object AvatarUtil {

    /**
     * Retrieve the first letter of a String.
     *
     * @param text String to obtain the first letter.
     * @return The first letter of the string to be painted in the default avatar.
     */
    @JvmStatic
    fun getFirstLetter(text: String): String {
        val resultUnknown = UNKNOWN_USER_NAME_AVATAR[0].toString().uppercase(Locale.getDefault())
        if (text.isEmpty()) {
            return resultUnknown
        }

        val trimmed = text.trim()
        if (trimmed.length == 1) {
            return trimmed[0].toString().uppercase(Locale.getDefault())
        }

        val resultTitle = EmojiUtilsShortcodes.emojify(trimmed)
        if (resultTitle.isNullOrBlank()) {
            return resultUnknown
        }

        val emojis = EmojiUtils.emojis(resultTitle)

        if (emojis.isNotEmpty() && emojis[0].start == 0) {
            return resultTitle.substring(emojis[0].start, emojis[0].end)
        }

        val resultEmojiCompat = hasEmojiCompatAtFirst(resultTitle)
        if (resultEmojiCompat != null) {
            return resultEmojiCompat
        }

        val resultChar = resultTitle[0].toString().uppercase(Locale.getDefault())
        if (resultChar.trim()
                .isEmpty() || resultChar == "(" || !isRecognizableCharacter(resultChar[0])
        ) {
            return resultUnknown
        }

        return resultChar
    }

    /**
     * Retrieve if a char is recognizable.
     *
     * @param inputChar The char to be examined.
     * @return True if the char is recognizable. Otherwise false.
     */
    private fun isRecognizableCharacter(inputChar: Char) =
        (inputChar.code in 48..57) || (inputChar.code in 65..90) || (inputChar.code in 97..122)

    private fun hasEmojiCompatAtFirst(text: String?): String? {
        if (text.isNullOrBlank()) {
            return null
        }

        try {
            val listEmojis = EmojiParser.extractEmojis(text)

            if (!listEmojis.isNullOrEmpty()) {
                val substring = text.substring(0, listEmojis[0].length)
                val sublistEmojis = EmojiParser.extractEmojis(substring)

                if (!sublistEmojis.isNullOrEmpty()) {
                    return substring
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    /**
     * Retrieve the color determined for an avatar.
     *
     * @param user The user from whom the color of the avatar is to be obtained.
     * @return The default avatar color.
     */
    @JvmStatic
    fun getColorAvatar(user: MegaUser?): Int {
        if (user == null) {
            return getColor(null)
        }

        val megaApi = MegaApplication.getInstance().megaApi
        return getColor(megaApi.getUserAvatarColor(user))
    }

    /**
     * Retrieve the color determined for an avatar.
     *
     * @param handle The identifier of the user from whom the color of the avatar is to be obtained.
     * @return The default avatar color.
     */
    @JvmStatic
    fun getColorAvatar(handle: Long): Int {
        if (handle == INVALID_HANDLE) {
            return getColor(null)
        }

        val megaApi = MegaApplication.getInstance().megaApi
        return getColor(megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(handle)))
    }

    /**
     * Retrieve the color determined for an avatar.
     *
     * @param handle The identifier of the user from whom the color of the avatar is to be obtained.
     * @return The default avatar color.
     */
    @JvmStatic
    fun getColorAvatar(handle: String?): Int {
        val avatarColor =
            handle?.let { MegaApplication.getInstance().megaApi.getUserAvatarColor(it) }
        return getColor(avatarColor)
    }

    private fun getColor(color: String?) =
        color?.let { Color.parseColor(it) } ?: getSpecificAvatarColor(AVATAR_PRIMARY_COLOR)

    /**
     * Retrieve the color of the avatar depending on the type.
     *
     * @param typeColor The kind of avatar that's going to be painted.
     * @return The color of the avatar in particular.
     */
    @JvmStatic
    fun getSpecificAvatarColor(typeColor: String): Int {
        val context: Context = MegaApplication.getInstance().currentActivity
            ?: MegaApplication.getInstance().baseContext

        return when (typeColor) {
            AVATAR_GROUP_CHAT_COLOR -> ContextCompat.getColor(context, R.color.grey_012_white_012)
            AVATAR_PHONE_COLOR -> ContextCompat.getColor(context, R.color.grey_500_grey_400)
            else -> ContextCompat.getColor(context, R.color.red_600_red_300)
        }
    }

    /**
     * Retrieve de default avatar.
     *
     * @param colorAvatar  The color of the avatar's background.
     * @param textAvatar   The letter to be painted on the avatar.
     * @param textSize     The size of the initial letter.
     * @param isList       Grid or list indicator.
     * @param customEmojis Indicator of whether or not to use mega emojis.
     * @return Bitmap with the default avatar built in.
     */
    @JvmStatic
    @JvmOverloads
    fun getDefaultAvatar(
        colorAvatar: Int,
        textAvatar: String?,
        textSize: Int,
        isList: Boolean,
        customEmojis: Boolean = true,
    ): Bitmap {
        val defaultAvatar = Bitmap.createBitmap(
            DEFAULT_AVATAR_WIDTH_HEIGHT,
            DEFAULT_AVATAR_WIDTH_HEIGHT,
            Bitmap.Config.ARGB_8888,
        )
        val c = Canvas(defaultAvatar)

        /*Background*/
        val paintCircle = Paint()
        paintCircle.color = colorAvatar
        paintCircle.isAntiAlias = true

        if (isList) {
            /*Shape list*/
            val radius = getRadius(defaultAvatar)
            c.drawCircle(
                (defaultAvatar.width / 2).toFloat(),
                (defaultAvatar.height / 2).toFloat(),
                radius.toFloat(),
                paintCircle,
            )
        } else {
            /*Shape grid*/
            val path = getRoundedRect(
                0f,
                0f,
                DEFAULT_AVATAR_WIDTH_HEIGHT.toFloat(),
                DEFAULT_AVATAR_WIDTH_HEIGHT.toFloat(),
                10f,
                10f,
                true,
                true,
                false,
                false,
            )
            c.drawPath(path, paintCircle)
        }

        /*Text*/
        val face = Typeface.SANS_SERIF
        val paintText = Paint()
        paintText.color = Color.WHITE
        paintText.textSize = textSize.toFloat()
        paintText.isAntiAlias = true
        paintText.textAlign = Paint.Align.CENTER
        paintText.typeface = face
        paintText.isAntiAlias = true
        paintText.isSubpixelText = true
        paintText.style = Paint.Style.FILL

        /*First Letter*/
        val resolvedText = if (textAvatar == null || textAvatar.trim().isEmpty()) {
            UNKNOWN_USER_NAME_AVATAR
        } else {
            textAvatar
        }

        val firstLetter = getFirstLetter(resolvedText)
        val firstEmoji = if (customEmojis) EmojiManager.getInstance().getFirstEmoji(firstLetter) else null
        if (firstEmoji != null) {
            val emojiBitmap = Bitmap.createScaledBitmap(
                firstEmoji.getBitmap(MegaApplication.getInstance()),
                textSize,
                textSize,
                false,
            )
            val xPos = (c.width - emojiBitmap.width) / 2
            val yPos = (c.height - emojiBitmap.height) / 2
            c.drawBitmap(emojiBitmap, xPos.toFloat(), yPos.toFloat(), paintText)
        } else {
            val bounds = Rect()
            paintText.getTextBounds(firstLetter, 0, firstLetter.length, bounds)
            val xPos = c.width / 2
            val yPos = ((c.height / 2) - ((paintText.descent() + paintText.ascent() / 2)) + 20).toInt()
            c.drawText(firstLetter.uppercase(Locale.getDefault()), xPos.toFloat(), yPos.toFloat(), paintText)
        }
        return defaultAvatar
    }

    /**
     * Retrieve the default avatar bitmap from Share Contact.
     *
     * @param context Context of the Activity.
     * @param contact The contact from whom the avatar is to be obtained.
     * @return Bitmap with the default avatar built in.
     */
    @JvmStatic
    fun getAvatarShareContact(context: Context, contact: ShareContactInfo): Bitmap {
        val mail = (context as AddContactActivity).getShareContactMail(contact)
        val color = when {
            contact.isPhoneContact -> ContextCompat.getColor(context, R.color.grey_500_grey_400)
            contact.isMegaContact -> getColorAvatar(contact.megaContactAdapter.megaUser)
            else -> getColor(null)
        }

        var fullName: String? = null
        if (contact.isPhoneContact) {
            fullName = contact.phoneContactInfo.name
        } else if (contact.isMegaContact) {
            fullName = contact.megaContactAdapter.fullName
        }
        if (fullName == null) {
            fullName = mail
        }

        if (contact.isPhoneContact || contact.isMegaContact) {
            /*Avatar*/
            val bitmap = getAvatarBitmap(mail)
            if (bitmap != null) {
                return getCircleBitmap(bitmap)
            }
        }

        /*Default Avatar*/
        return getDefaultAvatar(color, fullName, AVATAR_SIZE, true)
    }

    /**
     * Gets the bitmap of an avatar file.
     *
     * @param avatarName name of the avatar file
     * @return Bitmap of the avatar if the file exists.
     */
    @JvmStatic
    fun getAvatarBitmap(avatarName: String?): Bitmap? {
        return getAvatarBitmap(buildAvatarFile(avatarName + JPG_EXTENSION))
    }

    /**
     * Gets the bitmap of an avatar file given the File.
     *
     * @param avatar Avatar file.
     * @return The bitmap of the avatar if available.
     */
    @JvmStatic
    fun getAvatarBitmap(avatar: File?): Bitmap? {
        var bitmap: Bitmap? = null

        if (isFileAvailable(avatar) && (avatar?.length() ?: 0) > 0) {
            bitmap = BitmapFactory.decodeFile(avatar?.absolutePath, BitmapFactory.Options())
        }

        return bitmap
    }

    /**
     * Checks if already exists the avatar of a participant.
     * First with the handle and if not exists, then with the email.
     *
     * @param nameFileHandle participant's handle
     * @param nameFileEmail  participant's email
     * @return The participan's avatar if exists
     */
    @JvmStatic
    fun getUserAvatar(nameFileHandle: String?, nameFileEmail: String?): Bitmap? {
        var bitmap = getAvatarBitmap(nameFileHandle)

        if (bitmap == null) {
            bitmap = getAvatarBitmap(nameFileEmail)
        }

        return bitmap
    }

    /**
     * Sets the user's avatar
     *
     * @param handle          user's handle
     * @param email           user's email
     * @param fullName        user's full name
     * @param avatarImageView view in which the avatar has to be set
     */
    @JvmStatic
    fun setImageAvatar(handle: Long, email: String?, fullName: String?, avatarImageView: ImageView?) {
        if (avatarImageView == null) {
            return
        }

        val bitmap = getUserAvatar(MegaApiAndroid.userHandleToBase64(handle), email)
        if (bitmap != null) {
            avatarImageView.setImageBitmap(bitmap)
            return
        }

        avatarImageView.setImageBitmap(
            getDefaultAvatar(getColorAvatar(handle), fullName, AVATAR_SIZE, true),
        )
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun getCircleAvatar(context: Context, email: String?): Pair<Boolean, Bitmap>? {
        val avatar = buildAvatarFile(email + JPG_EXTENSION)
        if (!((isFileAvailable(avatar) && (avatar?.length() ?: 0) > 0))) {
            return Pair.create(false, null) as Pair<Boolean, Bitmap>
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(avatar?.absolutePath, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 250, 250)
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(avatar?.absolutePath, options)
        if (bitmap == null) {
            avatar?.delete()
            return Pair.create(false, null) as Pair<Boolean, Bitmap>
        }

        val circleBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888,
        )
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val paint = Paint()
        paint.shader = shader

        val canvas = Canvas(circleBitmap)
        val radius = getRadius(bitmap)

        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, radius.toFloat(), paint)
        return Pair.create(true, circleBitmap)
    }

    /**
     * Method for getting the radius of a bitmap to correctly paint the radius of the border.
     *
     * @param bitmap The bitmap.
     * @return The radius.
     */
    @JvmStatic
    fun getRadius(bitmap: Bitmap): Int {
        return if (bitmap.width < bitmap.height) {
            bitmap.width / 2
        } else {
            bitmap.height / 2
        }
    }

    /**
     * Gets the dominant color of a Bitmap.
     *
     * @param bitmap Bitmap to get its dominant color.
     * @return The dominant color.
     */
    @JvmStatic
    fun getDominantColor(bitmap: Bitmap): Int {

        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val pixels = IntArray(size)

        val bitmap2 = bitmap.copy(Bitmap.Config.ARGB_4444, false)

        bitmap2.getPixels(pixels, 0, width, 0, 0, width, height)

        val colorMap: List<HashMap<Int, Int>> = listOf(HashMap(), HashMap(), HashMap())

        var color: Int
        var r: Int
        var g: Int
        var b: Int
        var rC: Int?
        var gC: Int?
        var bC: Int?
        var j = 0

        while (j < pixels.size) {
            color = pixels[j]

            r = Color.red(color)
            g = Color.green(color)
            b = Color.blue(color)
            rC = colorMap[0][r]

            if (rC == null) {
                rC = 0
            }

            colorMap[0][r] = ++rC
            gC = colorMap[1][g]

            if (gC == null) {
                gC = 0
            }

            colorMap[1][g] = ++gC
            bC = colorMap[2][b]

            if (bC == null) {
                bC = 0
            }

            colorMap[2][b] = ++bC
            j = j + width + 1
        }

        val rgb = IntArray(3)

        for (i in 0..2) {
            var max = 0
            var value = 0

            for ((key, valueEntry) in colorMap[i]) {
                if (valueEntry > max) {
                    max = valueEntry
                    value = key
                }
            }

            rgb[i] = value
        }

        return Color.rgb(rgb[0], rgb[1], rgb[2])
    }

}
