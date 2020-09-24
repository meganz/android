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
    private BottomSheetBehavior mBehavior;

    protected View contentView;
    protected ViewGroup mainLinearLayout;
    protected ViewGroup items_layout;

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
     * @param heightHeader              height of the header
     * @param addBottomSheetCallBack    true if it should add a BottomsheetCallback, false otherwise
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
     * It depends on the number of options visibles on it
     * and on the display height and current orientation of the used device.
     * The maximum height will be a bit more than half of the screen.
     *
     * @return  The initial height of a BottomSheet
     */
    private int getPeekHeight() {
        int numOptions = items_layout.getChildCount();
        int numOptionsVisibles = 0;
        int heightChild = px2dp(HEIGHT_CHILD, outMetrics);
        int peekHeight = px2dp(heightHeader, outMetrics);

        for (int i = 0; i < numOptions; i++) {
            if (getItemsLayoutChildAt(i).getVisibility() == VISIBLE) {
                numOptionsVisibles++;
            }
        }

        if ((numOptionsVisibles <= 3 && heightHeader == HEIGHT_HEADER_LARGE) || (numOptionsVisibles <= 4 && heightHeader == HEIGHT_HEADER_LOW)) {
            return peekHeight + (heightChild * numOptions);
        } else {
            for (int i = 0; i < numOptions; i++) {
                if (isChildVisibleAt(i) && peekHeight < halfHeightDisplay) {
                    peekHeight += heightChild;

                    if (peekHeight >= halfHeightDisplay) {
                        if (getItemsLayoutChildAt(i + 2) != null) {
                            for (int j = i + 2; j < numOptions; j++) {
                                if (isChildVisibleAt(j)) {
                                    return peekHeight + (heightChild / 2);
                                }
                            }
                        } else if (isChildVisibleAt(i + 1)) {
                            return peekHeight + (heightChild / 2);
                        }

                        return peekHeight + heightChild;
                    }
                }
            }
        }

        return peekHeight;
    }

    /**
     * Gets a child view from "items_layout".
     *
     * @param index the index of the child to get
     * @return The child view
     */
    private View getItemsLayoutChildAt(int index) {
        return items_layout.getChildAt(index);
    }

    /**
     * Checks if a child view from "items_layout" exists and if it is visible.
     *
     * @param index the index of the child to check
     * @return True if the child view exists and if it is visible, false otherwise
     */
    private boolean isChildVisibleAt(int index) {
        return getItemsLayoutChildAt(index) != null && getItemsLayoutChildAt(index).getVisibility() == VISIBLE;
    }
}
