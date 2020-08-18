package mega.privacy.android.app.components;


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

import dagger.hilt.android.internal.managers.ViewComponentManager;

public class NewGridRecyclerView extends RecyclerView {
    
    private CustomizedGridLayoutManager manager;
    public int columnWidth = -1;
    private boolean isWrapContent = false;
    private int widthTotal = 0;
    private int spanCount = 2;
    
    public NewGridRecyclerView(Context context) {
        super(context);
        init(context, null);
    }
    
    public NewGridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public NewGridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }
        
        manager = new CustomizedGridLayoutManager(getContext(), 1);
        setLayoutManager(manager);
        calculateSpanCount();
    }
    
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        if(!isWrapContent){
            if (columnWidth > 0) {
                calculateSpanCount();
            }
        }
        else{
            ViewGroup.LayoutParams params = getLayoutParams();
            if (columnWidth > 0) {
                calculateSpanCount();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                setLayoutParams(params);
            }
        }
    }
    
    public void calculateSpanCount() {
        spanCount = Math.max(2, getScreenX() / columnWidth);
        manager.setSpanCount(spanCount);
    }
    
    private int getScreenX() {
        Point point = new Point();
        Context context = getContext();
        Activity activity = null;
        if (context instanceof ViewComponentManager.FragmentContextWrapper) {
            activity = ((ViewComponentManager.FragmentContextWrapper) getContext()).fragment.getActivity();
        } else {
            activity = ((Activity)context);
        }

        activity.getWindowManager().getDefaultDisplay().getSize(point);
        return point.x;
    }
    
    public int getSpanCount() {
        return spanCount;
    }
    
    public void setWrapContent(){
        isWrapContent = true;
        invalidate();
    }
    
    public int findFirstCompletelyVisibleItemPosition() {
        return getLayoutManager().findFirstCompletelyVisibleItemPosition();
    }
    
    public int findFirstVisibleItemPosition() {
        return getLayoutManager().findFirstVisibleItemPosition();
    }
    
    @Override
    public CustomizedGridLayoutManager getLayoutManager() {
        return manager;
    }

    /**
     * Empower the RecyclerView to change to Linear Layout as needed
     */
    public void switchToLinear() {
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * Turn back to use the well-configured CustomizedGridLayoutManager
     */
    public void switchBackToGrid() {
        setLayoutManager(manager);
        calculateSpanCount();
    }
}
