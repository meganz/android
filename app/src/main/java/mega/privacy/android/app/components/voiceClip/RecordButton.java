package mega.privacy.android.app.components.voiceClip;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class RecordButton extends AppCompatImageView implements View.OnTouchListener, View.OnClickListener {
    private RecordView recordView;
    private OnRecordClickListener onRecordClickListener;


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

    public static void log(String message) {
        Util.log("RecordButton", message);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);
        typedArray.recycle();

    }

    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
    }

    public void activateOnClickListener(boolean flag) {
        if (flag) {
            this.setOnClickListener(this);
        } else {
            this.setOnClickListener(null);
        }
    }

    public void activateOnTouchListener(boolean flag) {
        if (flag) {
            this.setOnTouchListener(this);
        } else {
            this.setOnTouchListener(null);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }

    public void setClip(View v) {
        if (v.getParent() == null) return;

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }
        if (v.getParent() instanceof View) {
            setClip((View) v.getParent());
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                log("onTouch() - ACTION_DOWN");
                recordView.onActionDown((RelativeLayout) v.getParent(), event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                log("onTouch() - ACTION_MOVE");
                recordView.onActionMove((RelativeLayout) v.getParent(), event);
                break;
            }
            case MotionEvent.ACTION_UP: {
                log("onTouch() - ACTION_UP");
                recordView.onActionUp((RelativeLayout) v.getParent());
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                log("onTouch() - ACTION_CANCEL");
                recordView.onActionCancel((RelativeLayout) v.getParent());
                break;
            }
            default: {
                log("onTouch() - default");
                break;
            }
        }
        return true;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        log("setOnRecordClickListener");
        this.onRecordClickListener = onRecordClickListener;
    }

    @Override
    public void onClick(View v) {
        if (onRecordClickListener == null) return;
        onRecordClickListener.onClick(v);
    }
}
