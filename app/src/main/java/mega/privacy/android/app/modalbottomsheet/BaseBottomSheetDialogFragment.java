package mega.privacy.android.app.modalbottomsheet;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final int HEIGHT_HEADER = 81;

    protected Context context;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;
    protected int heightDisplay;
    protected BottomSheetBehavior mBehavior;

    protected View contentView;
    protected LinearLayout mainLinearLayout;
    protected LinearLayout items_layout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MegaApplication app = MegaApplication.getInstance();
        if (app == null || getActivity() == null) {
            logError("MegaApplication or FragmentActivity is null");
            return;
        }

        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        dbH = app.getDbH();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    protected void setBottomSheetBehavior(boolean addBottomSheetCallBack) {
        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, HEIGHT_HEADER));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        if (addBottomSheetCallBack) {
            addBottomSheetCallBack();
        }
    }

    protected void setStateBottomSheetBehaviorHidden() {
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void addBottomSheetCallBack() {
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                    if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                        int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                        Rect rectangle = new Rect();
                        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.bottom;
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                        int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                        logDebug("bottomSheet.height: " + mainLinearLayout.getHeight() + " maxHeight: " + maxHeight);
                        if (mainLinearLayout.getHeight() > maxHeight) {
                            params.height = maxHeight;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }
}
