package mega.privacy.android.app.utils;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatMessage;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class TimeUtils implements Comparator<Calendar> {

    public static final int TIME = 0;
    public static final int DATE = TIME + 1;

    public static final int DATE_LONG_FORMAT = 0;
    public static final int DATE_SHORT_FORMAT = 1;
    public static final int DATE_SHORT_SHORT_FORMAT = 2;
    public static final int DATE_MM_DD_YYYY_FORMAT = 3;
    public static final int DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT = 4;
    private static final int TIME_OF_CHANGE = 8;
    private static final int INITIAL_PERIOD_TIME = 0;

    int type;

    public TimeUtils(int type){
        this.type = type;
    }

    public long calculateDifferenceDays(Calendar c1, Calendar c2){

        long diff = Math.abs(c1.getTimeInMillis() - c2.getTimeInMillis());
        long days = diff / (24 * 60 * 60 * 1000);
        return days;
    }

    @Override
    public int compare(Calendar c1, Calendar c2) {
        if(type==TIME){
            if (c1.get(Calendar.HOUR) != c2.get(Calendar.HOUR)){
                return c1.get(Calendar.HOUR) - c2.get(Calendar.HOUR);
            }
            else{
                long milliseconds1 = c1.getTimeInMillis();
                long milliseconds2 = c2.getTimeInMillis();

                long diff = milliseconds2 - milliseconds1;
//                long diffSeconds = diff / 1000;
                long diffMinutes = Math.abs(diff / (60 * 1000));

                if(diffMinutes<3){
                    return 0;
                }
                else{
                    return 1;
                }

                //            return c1.get(Calendar.MINUTE) - c2.get(Calendar.MINUTE);
            }
        }
        else if(type==DATE){
            if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR))
                return c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR);
            if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
                return c1.get(Calendar.MONTH) - c2.get(Calendar.MONTH);
            return c1.get(Calendar.DAY_OF_MONTH) - c2.get(Calendar.DAY_OF_MONTH);
        }
        return -1;
    }

    public static String formatTime(MegaChatMessage lastMessage){
        java.text.DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        Calendar cal = calculateDateFromTimestamp(lastMessage.getTimestamp());
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatDateAndTime(Context context, MegaChatMessage lastMessage, int format){

        java.text.DateFormat df;
        if(format == DATE_LONG_FORMAT){
            df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
        }
        else{
            df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        }

        Calendar cal = calculateDateFromTimestamp(lastMessage.getTimestamp());

        //Compare to yesterday
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeUtils tc = new TimeUtils(TimeUtils.DATE);
        if(tc.compare(cal, calToday)==0) {
            String time = formatTime(lastMessage);
            String formattedDate = context.getString(R.string.label_today) + " " + time;
            return formattedDate;
        }
        else if(tc.compare(cal, calYesterday)==0){
            String time = formatTime(lastMessage);
            String formattedDate = context.getString(R.string.label_yesterday) + " " + time;
            return formattedDate;
        }
        else{
            if(tc.calculateDifferenceDays(cal, calToday)<7){
                Date date = cal.getTime();
                String dayWeek = new SimpleDateFormat("EEEE").format(date);
                String time = formatTime(lastMessage);
                String formattedDate = dayWeek + " " + time;
                return formattedDate;
            }
            else{
                TimeZone tz = cal.getTimeZone();
                df.setTimeZone(tz);
                Date date = cal.getTime();
                String formattedDate = df.format(date);
                return formattedDate;
            }
        }
    }

    /**
     Gets a date formatted string from a timestamp.
     * @param context   Current context.
     * @param timestamp Timestamp in seconds to get the date formatted string.
     * @return The date formatted string.
     */
    public static String formatDate(Context context, long timestamp){
        return formatDate(context, timestamp, DATE_LONG_FORMAT, true);
    }

    /**
     * Gets a date formatted string from a timestamp.
     * @param context   Current context.
     * @param timestamp Timestamp in seconds to get the date formatted string.
     * @param format    Date format.
     * @return The date formatted string.
     */
    public static String formatDate(Context context, long timestamp, int format){
        return formatDate(context, timestamp, format, true);
    }

    /**
     * Gets a date formatted string from a timestamp.
     * @param context   Current context.
     * @param timestamp Timestamp in seconds to get the date formatted string.
     * @param format    Date format.
     * @param humanized Use humanized date format (i.e. today, yesterday or week day).
     * @return The date formatted string.
     */
    public static String formatDate(Context context, long timestamp, int format, boolean humanized) {

        Locale locale = Locale.getDefault();
        DateFormat df;

        switch (format) {
            case DATE_SHORT_FORMAT:
                df = new SimpleDateFormat("EEE d MMM", locale);
                break;
            case DATE_SHORT_SHORT_FORMAT:
                df = new SimpleDateFormat("d MMM", locale);
                break;
            case DATE_MM_DD_YYYY_FORMAT:
                df = new SimpleDateFormat("MMM d, yyyy", locale);
                break;
            case DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT:
                df = new SimpleDateFormat(getBestDateTimePattern (locale, "yyyy-MM-dd HH:mm"), locale);
                break;
            case DATE_LONG_FORMAT:
            default:
                df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, locale);
                break;
        }

        Calendar cal = calculateDateFromTimestamp(timestamp);

        if (humanized) {
            // Check if date is today, yesterday or less of a week
            Calendar calToday = Calendar.getInstance();
            Calendar calYesterday = Calendar.getInstance();
            calYesterday.add(Calendar.DATE, -1);
            TimeUtils tc = new TimeUtils(TimeUtils.DATE);

            if (tc.compare(cal, calToday) == 0) {
                return context.getString(R.string.label_today);
            } else if (tc.compare(cal, calYesterday) == 0) {
                return context.getString(R.string.label_yesterday);
            } else if (tc.calculateDifferenceDays(cal, calToday) < 7) {
                Date date = cal.getTime();
                return new SimpleDateFormat("EEEE", locale).format(date);
            }
        }

        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        return df.format(date);
    }

    public static String formatShortDateTime(long timestamp){

        java.text.DateFormat df;

        df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, Locale.getDefault());

        Calendar cal = calculateDateFromTimestamp(timestamp);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatLongDateTime(long timestamp){

        java.text.DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault());

        Calendar cal = calculateDateFromTimestamp(timestamp);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatTime(long ts){
        java.text.DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        Calendar cal = calculateDateFromTimestamp(ts);
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String lastGreenDate (Context context, int minutesAgo){
//        minutesAgo = 1442;
        Calendar calGreen = Calendar.getInstance();
        calGreen.add(Calendar.MINUTE, -minutesAgo);

        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeUtils tc = new TimeUtils(TimeUtils.DATE);
        long ts = calGreen.getTimeInMillis();
        logDebug("Ts last green: " + ts);
        if(minutesAgo>=65535){
            String formattedDate = context.getString(R.string.last_seen_long_time_ago);
            return formattedDate;
        }
        else if(tc.compare(calGreen, calToday)==0) {

            TimeZone tz = calGreen.getTimeZone();

            java.text.DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            df.setTimeZone(tz);

            String time = df.format(calGreen.getTime());

            String formattedDate =  context.getString(R.string.last_seen_today, time);

            return formattedDate;
        }
        //Impossible to fit yesterday
//        else if(tc.compare(calGreen, calYesterday)==0){
//            TimeZone tz = calGreen.getTimeZone();
//
//            java.text.DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
//            df.setTimeZone(tz);
//
//            String time = df.format(calGreen.getTime());
//
//            String formattedDate = "Last seen yesterday at" + " " + time;
//
//            return formattedDate;
//        }
        else{
            TimeZone tz = calGreen.getTimeZone();

            java.text.DateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            df.setTimeZone(tz);

            String time =df.format(calGreen.getTime());

            df = new SimpleDateFormat("dd MMM", Locale.getDefault());
            String day =df.format(calGreen.getTime());

            String formattedDate =  context.getString(R.string.last_seen_general, day, time);
            return formattedDate;
        }
    }

    public static String formatDateAndTime(Context context, long ts, int format){

        java.text.DateFormat df;
        if(format == DATE_LONG_FORMAT){
            df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
        }
        else{
            df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        }

        Calendar cal = calculateDateFromTimestamp(ts);

        //Compare to yesterday
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeUtils tc = new TimeUtils(TimeUtils.DATE);
        if(tc.compare(cal, calToday)==0) {
            String time = formatTime(ts);
            String formattedDate = context.getString(R.string.label_today) + " " + time;
            return formattedDate;
        }
        else if(tc.compare(cal, calYesterday)==0){
            String time = formatTime(ts);
            String formattedDate = context.getString(R.string.label_yesterday) + " " + time;
            return formattedDate;
        }
        else{
            if(tc.calculateDifferenceDays(cal, calToday)<7){
                Date date = cal.getTime();
                String dayWeek = new SimpleDateFormat("EEEE").format(date);
                String time = formatTime(ts);
                String formattedDate = dayWeek + " " + time;
                return formattedDate;
            }
            else{
                TimeZone tz = cal.getTimeZone();
                df.setTimeZone(tz);
                Date date = cal.getTime();
                String formattedDate = df.format(date);
                return formattedDate;
            }
        }
    }

    public static String getDateString(long date){
        DateFormat datf = DateFormat.getDateTimeInstance();
        String dateString = "";

        dateString = datf.format(new Date(date*1000));

        return dateString;
    }

    public static String formatBucketDate(Context context, long ts) {
        Calendar cal = Util.calculateDateFromTimestamp(ts);
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeUtils tc = new TimeUtils(TimeUtils.DATE);

        if (tc.compare(cal, calToday) == 0) {
            return context.getString(R.string.label_today);
        } else if (tc.compare(cal, calYesterday) == 0) {
            return context.getString(R.string.label_yesterday);
        } else {
            Date date = cal.getTime();
            return new SimpleDateFormat("EEEE, d MMM yyyy").format(date);
        }
    }

    public static String getVideoDuration(int duration) {
        if (duration > 0) {
            int hours = duration / 3600;
            int minutes = (duration % 3600) / 60;
            int seconds = duration % 60;

            if (hours > 0) {
                return String.format("%d:%d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%d:%02d", minutes, seconds);
            }
        }

        return null;
    }

    /**
     * Method for obtaining the appropriate String depending on the current time.
     *
     * @param context Activity context.
     * @param option  Selected mute type.
     * @return The right string.
     */
    public static String getCorrectStringDependingOnCalendar(Context context, String option) {
        Calendar calendar = getCalendarSpecificTime(option);
        TimeZone tz = calendar.getTimeZone();
        java.text.DateFormat df = new SimpleDateFormat("h", Locale.getDefault());
        df.setTimeZone(tz);
        String time = df.format(calendar.getTime());
        return option.equals(NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING) ?
                context.getString(R.string.success_muting_a_chat_until_this_morning, time) :
                context.getString(R.string.success_muting_a_chat_until_tomorrow_morning, context.getString(R.string.label_tomorrow).toLowerCase(), time);
    }

    /**
     * Method for obtaining the appropriate String depending on the option selected.
     *
     * @param timestamp The time in minutes that notifications of a chat or all chats are muted.
     * @return The right string
     */
    public static String getCorrectStringDependingOnOptionSelected(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000);

        Calendar calToday = Calendar.getInstance();
        calToday.setTimeInMillis(System.currentTimeMillis());

        Calendar calTomorrow = Calendar.getInstance();
        calTomorrow.add(Calendar.DATE, +1);

        java.text.DateFormat df;
        Locale locale = MegaApplication.getInstance().getBaseContext().getResources().getConfiguration().locale;
        df = new SimpleDateFormat(getBestDateTimePattern(locale, "HH:mm"), locale);

        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        return MegaApplication.getInstance().getString(R.string.chat_notifications_muted_today, df.format(cal.getTime()));
    }

    /**
     * Method for obtaining a calendar depending on the type of silencing chosen.
     * @param option Selected mute type.
     * @return The Calendar.
     */
    public static Calendar getCalendarSpecificTime(String option) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, TIME_OF_CHANGE);
        calendar.set(Calendar.AM_PM, Calendar.AM);

        if(option.equals(NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING)){
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar;
    }

    /**
     * Method to know if the silencing should be until this morning.
     *
     * @return True if it is. False it is not.
     */
    public static boolean isUntilThisMorning() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return hour < TIME_OF_CHANGE || (hour == TIME_OF_CHANGE && minute == INITIAL_PERIOD_TIME);
    }

    /**
     * Converts seconds time into a humanized format string.
     * - If time is greater than a DAY, the formatted string will be "X day(s)".
     * - If time is lower than a DAY and greater than a HOUR, the formatted string will be "Xh Ym".
     * - If time is lower than a HOUR and greater than a MINUTE, the formatted string will be "Xm Ys".
     * - If time is lower than a MINUTE, the formatted string will be "Xs".
     *
     * @param time Time in seconds to get the formatted string.
     * @return The humanized format string.
     */
    public static String getHumanizedTime(long time) {
        Context context = MegaApplication.getInstance().getApplicationContext();
        if (time <= 0) {
            return context.getString(R.string.label_time_in_seconds, 0);
        }

        long days = TimeUnit.SECONDS.toDays(time);
        long hours = TimeUnit.SECONDS.toHours(time) - TimeUnit.DAYS.toHours(days);
        long minutes = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.DAYS.toMinutes(days) + TimeUnit.HOURS.toMinutes(hours));
        long seconds = TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.DAYS.toSeconds(days) + TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(minutes));

        if (days > 0) {
            return context.getResources().getQuantityString(R.plurals.label_time_in_days_full, (int)days, (int)days);
        } else if (hours > 0) {
            return context.getString(R.string.label_time_in_hours, hours) + " " +
                    context.getString(R.string.label_time_in_minutes, minutes);
        } else if (minutes > 0) {
            return context.getString(R.string.label_time_in_minutes, minutes) + " " +
                    context.getString(R.string.label_time_in_seconds, seconds);
        } else {
            return context.getString(R.string.label_time_in_seconds, seconds);
        }
    }

    /*
     * Converts milliseconds time into a humanized format string.
     * - If time is greater than a DAY, the formatted string will be "X day(s)".
     * - If time is lower than a DAY and greater than a HOUR, the formatted string will be "Xh Ym".
     * - If time is lower than a HOUR and greater than a MINUTE, the formatted string will be "Xm Ys".
     * - If time is lower than a MINUTE, the formatted string will be "Xs".
     *
     * @param time Time in milliseconds to get the formatted string.
     * @return The humanized format string.
     */
    public static String getHumanizedTimeMs(long time) {
        return getHumanizedTime(TimeUnit.MILLISECONDS.toSeconds(time));
    }

    /**
     * Shows and manages a countdown timer in a view.
     *
     * Note:    The view can be an AlertDialog or any other type of View.
     *          - If the view is an AlertDialog, it can be:
     *              * Simple, which does not need any other view received by param.
     *              * Customized, which must contain a TextView received by param.
     *          - If the view is any other type of View, it must contain a TextView received by param.
     *
     * @param stringResource    string resource in which the timer has to be shown
     * @param alertDialog       warning dialog in which the timer has to be shown
     * @param v                 View in which the timer has to be shown
     * @param textView          TextView in which the string resource has to be set
     */
    public static void createAndShowCountDownTimer(int stringResource, AlertDialog alertDialog, View v, TextView textView) {
        Context context = MegaApplication.getInstance().getApplicationContext();
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();

        new CountDownTimer(megaApi.getBandwidthOverquotaDelay(), 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                String textToShow = context.getString(stringResource, getHumanizedTimeMs(millisUntilFinished));

                if (textView == null) {
                    alertDialog.setMessage(textToShow);
                } else {
                    textView.setText(textToShow);
                }
            }

            @Override
            public void onFinish() {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                } else if (v != null) {
                    v.setVisibility(View.GONE);
                }
            }
        }.start();
    }

    /**
     * Shows and manages a countdown timer in a warning dialog.
     *
     * @param alertDialog       warning dialog in which the timer has to be shown
     * @param stringResource    string resource in which the timer has to be shown
     * @param textView          TextView in which the string resource has to be set
     */
    public static void createAndShowCountDownTimer(int stringResource, AlertDialog alertDialog, TextView textView) {
        createAndShowCountDownTimer(stringResource, alertDialog, null, textView);
    }

    /**
     * Shows and manages a countdown timer in a warning dialog.
     *
     * @param alertDialog       warning dialog in which the timer has to be shown
     * @param stringResource    string resource in which the timer has to be shown
     */
    public static void createAndShowCountDownTimer(int stringResource, AlertDialog alertDialog) {
        createAndShowCountDownTimer(stringResource, alertDialog, null, null);
    }

    /**
     * Shows and manages a countdown timer in a view.
     *
     * @param stringResource    string resource in which the timer has to be shown
     * @param textView          TextView in which the string resource has to be set
     */
    public static void createAndShowCountDownTimer(int stringResource, View v, TextView textView) {
        createAndShowCountDownTimer(stringResource, null, v, textView);
    }
}
