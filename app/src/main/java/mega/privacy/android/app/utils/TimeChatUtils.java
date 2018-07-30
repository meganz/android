package mega.privacy.android.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import nz.mega.sdk.MegaChatMessage;

public class TimeChatUtils implements Comparator<Calendar> {

    public static int TIME=0;
    public static int DATE=TIME+1;

    public static int DATE_LONG_FORMAT=0;
    public static int DATE_SHORT_FORMAT=1;
    public static int DATE_SHORT_SHORT_FORMAT=2;

    int type;

    public TimeChatUtils(int type){
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
        Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getTimestamp());
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatDateAndTime(MegaChatMessage lastMessage, int format){

        java.text.DateFormat df;
        if(format == DATE_LONG_FORMAT){
            df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
        }
        else{
            df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        }

        Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getTimestamp());

        //Compare to yesterday
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);
        if(tc.compare(cal, calToday)==0) {
            String time = formatTime(lastMessage);
            String formattedDate = "Today" + " " + time;
            return formattedDate;
        }
        else if(tc.compare(cal, calYesterday)==0){
            String time = formatTime(lastMessage);
            String formattedDate = "Yesterday" + " " + time;
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

    public static String formatDate(long timestamp, int format){

        java.text.DateFormat df;
        if(format == DATE_LONG_FORMAT){
            df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
        }else if(format == DATE_SHORT_SHORT_FORMAT){
            df = new SimpleDateFormat("d MMM");

        }else{
            df = new SimpleDateFormat("EEE d MMM");

            //df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        }

        Calendar cal = Util.calculateDateFromTimestamp(timestamp);

        //Compare to yesterday
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);
        if(tc.compare(cal, calToday)==0) {
            return "Today";
        }
        else if(tc.compare(cal, calYesterday)==0){
            return "Yesterday";
        }
        else{
            if(tc.calculateDifferenceDays(cal, calToday)<7){
                Date date = cal.getTime();
                String dayWeek = new SimpleDateFormat("EEEE").format(date);
                return dayWeek;
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

    public static String formatShortDateTime(long timestamp){

        java.text.DateFormat df;

        df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, Locale.getDefault());

        Calendar cal = Util.calculateDateFromTimestamp(timestamp);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatTime(long ts){
        java.text.DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        Calendar cal = Util.calculateDateFromTimestamp(ts);
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        return formattedDate;
    }

    public static String formatDateAndTime(long ts, int format){

        java.text.DateFormat df;
        if(format == DATE_LONG_FORMAT){
            df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
        }
        else{
            df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        }

        Calendar cal = Util.calculateDateFromTimestamp(ts);

        //Compare to yesterday
        Calendar calToday = Calendar.getInstance();
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DATE, -1);
        TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);
        if(tc.compare(cal, calToday)==0) {
            String time = formatTime(ts);
            String formattedDate = "Today" + " " + time;
            return formattedDate;
        }
        else if(tc.compare(cal, calYesterday)==0){
            String time = formatTime(ts);
            String formattedDate = "Yesterday" + " " + time;
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

    private static void log(String message) {
        Util.log("TimeChatUtils", message);
    }
}
