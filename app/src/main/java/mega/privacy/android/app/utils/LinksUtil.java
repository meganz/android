package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.activities.GetLinkActivity;
import mega.privacy.android.app.listeners.SessionTransferURLListener;

import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.MEGA_REGEXS;
import static mega.privacy.android.app.utils.Constants.OPENED_FROM_CHAT;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class LinksUtil {

    private static final String REQUIRES_TRANSFER_SESSION = "fm/";

    private static boolean isClickAlreadyIntercepted;

    /**
     * Checks if the link received requires transfer session.
     *
     * @param url   link to check
     * @return True if the link requires transfer session, false otherwise.
     */
    public static boolean requiresTransferSession(Context context, String url) {
        if (url.contains(REQUIRES_TRANSFER_SESSION)) {
            int start = url.indexOf(REQUIRES_TRANSFER_SESSION);
            if (start != -1) {
                String path = url.substring(start + REQUIRES_TRANSFER_SESSION.length());
                if (!isTextEmpty(path)) {
                    MegaApplication.getInstance().getMegaApi().getSessionTransferURL(path, new SessionTransferURLListener(context));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the url is a MEGA link and if it requires transfer session.
     *
     * @param context   current Context
     * @param url       link to check
     * @return True if the link is a MEGA link and requires transfer session, false otherwise.
     */
    public static boolean isMEGALinkAndRequiresTransferSession(Context context, String url) {
        return !isTextEmpty(url) && matchRegexs(url, MEGA_REGEXS) && requiresTransferSession(context, url);
    }

    /**
     * Sets a customized onClick listener in a TextView to intercept click events on links:
     * - If the link requires transfer session, requests it.
     * - If not, launches a general ACTION_VIEW intent.
     *
     * @param context       current Context
     * @param strBuilder    SpannableStringBuilder containing the text of the TextView
     * @param span          URLSpan containing the links
     */
    public static void makeLinkClickable(Context context, SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);

        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                isClickAlreadyIntercepted = true;

                String url = span.getURL();
                if (isTextEmpty(url)) return;

                if (!isMEGALinkAndRequiresTransferSession(context, url)) {
                    Uri uri = Uri.parse(url);
                    if (uri == null) {
                        logWarning("Uri is null. Cannot open the link.");
                        return;
                    }

                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri)
                            .putExtra(OPENED_FROM_CHAT, true));
                }
            }
        };

        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    /**
     * Checks if the content of the TextView has links.
     * If so, sets a customized onClick listener to intercept the clicks on them.
     *
     * @param context   current Context
     * @param textView  TextView to check
     */
    public static void interceptLinkClicks(Context context, TextView textView) {
        CharSequence sequence = textView.getText();
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(context, strBuilder, span);
        }
        textView.setText(strBuilder);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static boolean isIsClickAlreadyIntercepted() {
        return isClickAlreadyIntercepted;
    }

    public static void resetIsClickAlreadyIntercepted() {
        LinksUtil.isClickAlreadyIntercepted = false;
    }

    /**
     * Splits the link from its decryption key and returns the link.
     *
     * @param linkWithKey The link with the decryption key.
     * @return The link without the decryption key.
     */
    public static String getLinkWithoutKey(String linkWithKey) {
        if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
            //old file or folder link format
            String[] s = linkWithKey.split("!");

            if (s.length == 3) {
                return s[0] + "!" + s[1];
            }
        } else {
            // new file or folder link format
            String[] s = linkWithKey.split("#");

            if (s.length == 2) {
                return s[0];
            }
        }

        return null;
    }

    /**
     * Splits the link from its decryption key and returns the decryption key.
     *
     * @param linkWithKey The link with the decryption key
     * @return The decryption key of the link.
     */
    public static String getKeyLink(String linkWithKey) {
        if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
            //old file or folder link format
            String[] s = linkWithKey.split("!");

            if (s.length == 3) {
                return s[2];
            }
        } else {
            // new file or folder link format
            String[] s = linkWithKey.split("#");

            if (s.length == 2) {
                return s[1];
            }
        }

        return null;
    }

    /**
     * Launches an intent to show get link activity.
     *
     * @param activity Activity which launches the intent.
     * @param handle   identifier of the node to get or manage its link.
     */
    public static void showGetLinkActivity(Activity activity, long handle) {
        activity.startActivity(new Intent(activity, GetLinkActivity.class)
                .putExtra(HANDLE, handle));
    }
}
