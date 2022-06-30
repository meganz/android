package mega.privacy.android.app.components.voiceClip;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;

import mega.privacy.android.app.R;
import timber.log.Timber;

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
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                Timber.d("onTouch() - ACTION_DOWN");
                recordView.onActionDown((RelativeLayout) v.getParent(), event);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                Timber.d("onTouch() - ACTION_MOVE");
                recordView.onActionMove((RelativeLayout) v.getParent(), event);
                break;
            }
            case MotionEvent.ACTION_UP: {
                Timber.d("onTouch() - ACTION_UP");
                recordView.onActionUp((RelativeLayout) v.getParent());
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                Timber.d("onTouch() - ACTION_CANCEL");
                recordView.onActionCancel((RelativeLayout) v.getParent());
                break;
            }
            default: {
                Timber.d("onTouch() - default");
                break;
            }
        }
        return true;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        Timber.d("setOnRecordClickListener");
        this.onRecordClickListener = onRecordClickListener;
    }

    @Override
    public void onClick(View v) {
        if (onRecordClickListener == null) return;
        onRecordClickListener.onClick(v);
    }
}
