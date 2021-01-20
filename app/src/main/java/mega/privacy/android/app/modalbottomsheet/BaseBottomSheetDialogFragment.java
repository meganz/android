package mega.privacy.android.app.modalbottomsheet;

import android.content.Context;
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

import static android.view.View.VISIBLE;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final int DEFAULT_VIEW_TYPE = 0;
    private static final int RADIO_GROUP_VIEW_TYPE = 1;

    private static final int HEIGHT_RADIO_GROUP_VIEW = 56;
    private static final int HEIGHT_CHILD = 50;
    protected static final int HEIGHT_HEADER_LARGE = 81;
    protected static final int HEIGHT_HEADER_LOW = 48;

    protected Context context;
    protected MegaApplication app;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;
    private int halfHeightDisplay;
    private int heightHeader;
    protected BottomSheetBehavior mBehavior;

    private int viewType = DEFAULT_VIEW_TYPE;
    protected View contentView;
    protected LinearLayout mainLinearLayout;
    protected LinearLayout items_layout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = MegaApplication.getInstance();
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

        halfHeightDisplay = outMetrics.heightPixels / 2;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    /**
     * Sets the initial state of a BottomSheet and its state.
     *
     * @param heightHeader           Height of the header.
     * @param addBottomSheetCallBack True if it should add a BottomsheetCallback, false otherwise.
     */
    protected void setBottomSheetBehavior(int heightHeader, boolean addBottomSheetCallBack) {
        this.heightHeader = heightHeader;
        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());

        int peekHeight = getPeekHeight();
        if (peekHeight < halfHeightDisplay) {
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mBehavior.setPeekHeight(peekHeight);
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        if (addBottomSheetCallBack) {
            addBottomSheetCallBack();
        }
    }

    /**
     * Sets the initial state of a BottomSheet composed by a RadioGroup and its state.
     *
     */
    protected void setRadioGroupViewBottomSheetBehaviour() {
        viewType = RADIO_GROUP_VIEW_TYPE;
        setBottomSheetBehavior(HEIGHT_RADIO_GROUP_VIEW, false);
    }

    /**
     * Hides the BottomSheet.
     */
    protected void setStateBottomSheetBehaviorHidden() {
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * Sets the behaviour of a BottomSheet when its state changes.
     */
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
                if (isScreenInPortrait(context)) {
                    ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                    if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                        int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                        Rect rectangle = new Rect();
                        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.bottom;
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                        int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                        if (mainLinearLayout.getHeight() > maxHeight) {
                            params.height = maxHeight;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    /**
     * Gets the initial height of a BottomSheet.
     * It depends on the number of visible options on it, the display height
     * and current orientation of the used device.
     * The maximum height will be a bit more than half of the screen.
     *
     * @return The initial height of a BottomSheet.
     */
    private int getPeekHeight() {
        int numVisibleOptions = 0;
        int heightChild = dp2px(viewType == DEFAULT_VIEW_TYPE ? HEIGHT_CHILD : HEIGHT_RADIO_GROUP_VIEW, outMetrics);
        int peekHeight = dp2px(heightHeader, outMetrics);

        for (int i = 0; i < items_layout.getChildCount(); i++) {
            if (isChildVisibleAt(i)) {
                numVisibleOptions++;
            }
        }

        if ((numVisibleOptions <= 3 && heightHeader == HEIGHT_HEADER_LARGE)
                || (numVisibleOptions <= 4 && heightHeader == HEIGHT_HEADER_LOW)) {
            return peekHeight + (heightChild * numVisibleOptions);
        } else {
            for (int i = 1; i <= numVisibleOptions; i++) {
                if (peekHeight < halfHeightDisplay) {
                    peekHeight += heightChild;

                    if (peekHeight >= halfHeightDisplay) {
                        int nextVisiblePosition = i + 1;

                        if (nextVisiblePosition == numVisibleOptions) {
                            return peekHeight + heightChild;
                        } else {
                            return peekHeight + (heightChild / 2);
                        }
                    }
                }
            }
        }

        return peekHeight;
    }

    /**
     * Checks if a child view from "items_layout" exists and if it is visible.
     *
     * @param index the index of the child to check
     * @return True if the child view exists and if it is visible, false otherwise
     */
    private boolean isChildVisibleAt(int index) {
        View v = items_layout.getChildAt(index);

        return v != null && v.getVisibility() == VISIBLE;
    }
}
