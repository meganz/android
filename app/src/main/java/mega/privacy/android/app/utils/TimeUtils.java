package mega.privacy.android.app.utils;

import android.content.Context;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
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

    public static String formatDate(Context context, long timestamp, int format){

        java.text.DateFormat df;

        switch (format) {
            case DATE_LONG_FORMAT:
                df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
                break;
            case DATE_SHORT_FORMAT:
                df = new SimpleDateFormat("EEE d MMM");
                break;
            case DATE_SHORT_SHORT_FORMAT:
                df = new SimpleDateFormat("d MMM");
                break;
            case DATE_MM_DD_YYYY_FORMAT:
                df = new SimpleDateFormat("MMM d, YYYY");
                break;
            case DATE_AND_TIME_YYYY_MM_DD_HH_MM_FORMAT:
                Locale locale = context.getResources().getConfiguration().locale;
                df = new SimpleDateFormat(getBestDateTimePattern (locale, "YYYY-MM-dd HH:mm"), locale);
                break;
            default:
                df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
                break;
        }

        Calendar cal = calculateDateFromTimestamp(timestamp);

        //Compare to yesterday
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
            String dayWeek = new SimpleDateFormat("EEEE").format(date);
            return dayWeek;
        } else {
            TimeZone tz = cal.getTimeZone();
            df.setTimeZone(tz);
            Date date = cal.getTime();
            String formattedDate = df.format(date);
            return formattedDate;
        }
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

    public static Calendar getCalendarSpecificTime(String typeTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 7);

        if(typeTime.equals(NOTIFICATIONS_DISABLED_UNTIL_THIS_EVENING)){
            calendar.set(Calendar.AM_PM, Calendar.PM);
        }else{
            calendar.set(Calendar.AM_PM, Calendar.AM);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar;
    }

    public static boolean isUntilThisEvening(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if(hour <= 18 && hour >= 4) {
           return true;
        }
        return false;
    }

    public static String mutedChatNotification(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);

        Calendar calToday = Calendar.getInstance();
        calToday.setTimeInMillis(System.currentTimeMillis());

        Calendar calTomorrow = Calendar.getInstance();
        calTomorrow.add(Calendar.DATE, +1);

        TimeUtils tc = new TimeUtils(TimeUtils.DATE);
        java.text.DateFormat df;
        Locale locale = MegaApplication.getInstance().getBaseContext().getResources().getConfiguration().locale;
        df = new SimpleDateFormat(getBestDateTimePattern(locale, "HH:mm"), locale);

        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        if (tc.compare(cal, calToday) == 0) {
            return MegaApplication.getInstance().getString(R.string.chat_notifications_muted_today, formattedDate);
        }

        return MegaApplication.getInstance().getString(R.string.chat_notifications_muted_tomorrow, formattedDate);
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

    public static String getCorrectStringDependingOnCalendar(Context context, String typeMuted) {
        Calendar calendar = getCalendarSpecificTime(typeMuted);
        TimeZone tz = calendar.getTimeZone();
        java.text.DateFormat df = new SimpleDateFormat("h", Locale.getDefault());
        df.setTimeZone(tz);
        String time = df.format(calendar.getTime());

        return typeMuted.equals(NOTIFICATIONS_DISABLED_UNTIL_THIS_EVENING) ?
                context.getString(R.string.success_muting_a_chat_until_this_evening, time) :
                context.getString(R.string.success_muting_a_chat_until_tomorrow_morning, context.getString(R.string.label_tomorrow), time);
    }
}
