package mega.privacy.android.app.components.voiceClip;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;


public class RecordButton extends AppCompatImageView implements View.OnTouchListener, View.OnClickListener {
//    private ScaleAnim scaleAnim;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;


    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
    }

    public RecordButton(Context context) {
        super(context);
        init(context, null);
    }
    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public RecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);
            int imageResource = typedArray.getResourceId(R.styleable.RecordButton_mic_icon, -1);

            if (imageResource != -1) {
                setTheImageResource(imageResource);
            }
            typedArray.recycle();
        }
        this.setOnTouchListener(this);
        this.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }
    public void setClip(View v) {
        if (v.getParent() == null) {
            return;
        }
        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }
        if (v.getParent() instanceof View) {
            setClip((View) v.getParent());
        }
    }

    private void setTheImageResource(int imageResource) {
        Drawable image = AppCompatResources.getDrawable(getContext(), imageResource);
        setImageDrawable(image);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isListenForRecord()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    log("onTouch() - ACTION_DOWN");
                    recordView.onActionDown((RecordButton) v, event);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    log("onTouch() - ACTION_MOVE");
                    recordView.onActionMove((RecordButton) v, event);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    log("onTouch() - ACTION_UP");
                    recordView.onActionUp((RecordButton) v);
                    break;
                }
            }
        }
        return isListenForRecord();
    }


    public void setListenForRecord(boolean listenForRecord) {
        log("setListenForRecord() -> "+listenForRecord);
        this.listenForRecord = listenForRecord;
    }
    public boolean isListenForRecord() {
        log("isListenForRecord() -> "+listenForRecord);
        return listenForRecord;
    }
    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        log("setOnRecordClickListener()");
        this.onRecordClickListener = onRecordClickListener;
    }

    @Override
    public void onClick(View v) {
        if (onRecordClickListener != null){
            log("onClick()");
            onRecordClickListener.onClick(v);
        }
    }

    public static void log(String message) {
        Util.log("RecordButton",message);
    }
}
