package mega.privacy.android.app.utils;

import java.util.Calendar;
import java.util.Comparator;

public class TimeComparator implements Comparator<Calendar> {

    public static int TIME=0;
    public static int DATE=TIME+1;

    int type;

    public TimeComparator(int type){
        this.type = type;
    }

    @Override
    public int compare(Calendar c1, Calendar c2) {
        if(type==TIME){
            if (c1.get(Calendar.HOUR) != c2.get(Calendar.HOUR))
                return c1.get(Calendar.HOUR) - c2.get(Calendar.HOUR);
            return c1.get(Calendar.MINUTE) - c2.get(Calendar.MINUTE);
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
}
