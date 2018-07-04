package mega.privacy.android.app.modalbottomsheet;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 22/06/18.
 */

public class UtilsModalBottomSheet {

    public static int getPeekHeight (LinearLayout items_layout, int heightDisplay, Context context, int heightHeader) {
        int numOptions = items_layout.getChildCount();
        int numOptionsVisibles = 0;
        int heightScreen = (heightDisplay / 2);
        int heightChild = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        int peekHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightHeader, context.getResources().getDisplayMetrics());

        for (int i=0; i<numOptions; i++){
            if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE) {
                numOptionsVisibles++;
            }
        }

        if ((numOptionsVisibles <= 3 && heightHeader == 81) || (numOptionsVisibles <= 4 && heightHeader == 48)){
            peekHeight += (heightChild * numOptions);
        }
        else {
            for (int i = 0; i < numOptions; i++) {
                if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE && peekHeight < heightScreen) {
                    log("Child i: " + i + " is visible; peekHeight: " + peekHeight + " heightScreen: " + heightScreen + " heightChild: " + heightChild);
                    peekHeight += heightChild;
                    if (peekHeight >= heightScreen) {
                        if (items_layout.getChildAt(i + 2) != null) {
                            boolean visible = false;
                            for (int j = i + 2; j < numOptions; j++) {
                                if (items_layout.getChildAt(j).getVisibility() == View.VISIBLE) {
                                    visible = true;
                                    break;
                                }
                            }
                            if (visible) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else if (items_layout.getChildAt(i + 1) != null) {
                            if (items_layout.getChildAt(i + 1).getVisibility() == View.VISIBLE) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else {
                            peekHeight += heightChild;
                            break;
                        }
                    }
                }
            }
        }
        return peekHeight;
    }

    private static void log(String log) {
        Util.log("UtilsModalBottomSheet", log);
    }
}
