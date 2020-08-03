package mega.privacy.android.app.lollipop.megachat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.core.content.ContextCompat;
import java.util.Objects;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

public class BadgeDrawerArrowDrawable extends DrawerArrowDrawable {

    // Fraction of the drawable's intrinsic size we want the badge to be.
    private static final float SIZE_FACTOR = .5f;
    private static final float HALF_SIZE_FACTOR = SIZE_FACTOR / 4;

    private Paint backgroundPaint;
    private Paint bigBackgroundPaint;
    private Paint textPaint;
    private String text;
    private boolean showDot;
    private boolean badgeEnabled = true;

    public BadgeDrawerArrowDrawable(Context context) {
        super(context);

        backgroundPaint = new Paint();
        if (context instanceof ManagerActivityLollipop
            || context instanceof ArchivedChatsActivity) {
            backgroundPaint.setColor(ContextCompat.getColor(context, R.color.dark_primary_color));
        } else {
            backgroundPaint.setColor(Color.WHITE);
        }
        backgroundPaint.setAntiAlias(true);

        bigBackgroundPaint = new Paint();
        if (context instanceof ManagerActivityLollipop
            || context instanceof ArchivedChatsActivity) {
            bigBackgroundPaint.setColor(Color.WHITE);
        } else {
            bigBackgroundPaint.setColor(
                ContextCompat.getColor(context, R.color.dark_primary_color));
        }
        bigBackgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        if (context instanceof ManagerActivityLollipop
            || context instanceof ArchivedChatsActivity) {
            textPaint.setColor(Color.WHITE);
        } else {
            textPaint.setColor(ContextCompat.getColor(context, R.color.dark_primary_color));
        }
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(SIZE_FACTOR * getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (!badgeEnabled) {
            return;
        }

        final Rect bounds = getBounds();

        if (showDot) {
            bigBackgroundPaint.setAlpha((int) ((1 - getProgress()) * 255));
            backgroundPaint.setAlpha((int) ((1 - getProgress()) * 255));

            final float x = (1 - 0.2f) * bounds.width();
            final float y = 0.25f * bounds.height();
            canvas.drawCircle(x, y, (0.25f / 1.4f) * bounds.width(), bigBackgroundPaint);

            final float x1 = (1 - 0.2f) * bounds.width() + 2;
            final float y1 = 0.25f * bounds.height() - 2;
            canvas.drawCircle(x1, y1, (0.25f / 1.3f) * bounds.width() - 2, backgroundPaint);

            return;
        }

        if (text == null || text.length() == 0) {
            return;
        }

        final float x = (1 - HALF_SIZE_FACTOR) * bounds.width();
        final float y = HALF_SIZE_FACTOR * bounds.height();
        canvas.drawCircle(x, y, (SIZE_FACTOR / 1.4f) * bounds.width(), bigBackgroundPaint);

        final float x1 = (1 - HALF_SIZE_FACTOR) * bounds.width() + 2;
        final float y1 = HALF_SIZE_FACTOR * bounds.height() - 2;
        canvas.drawCircle(x1, y1, (SIZE_FACTOR / 1.3f) * bounds.width() - 2, backgroundPaint);

        final Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, x1, y1 + (float) (textBounds.height() / 2.5), textPaint);
    }

    public void setBadgeEnabled(boolean badgeEnabled) {
        if (this.badgeEnabled != badgeEnabled) {
            this.badgeEnabled = badgeEnabled;
            invalidateSelf();
        }
    }

    public void setText(String text) {
        if (!Objects.equals(this.text, text)) {
            this.text = text;
            invalidateSelf();
        }
    }

    public String getText() {
        return text;
    }

    public void setShowDot(boolean showDot) {
        this.showDot = showDot;
    }

    public void setBackgroundColor(int color) {
        if (backgroundPaint.getColor() != color) {
            backgroundPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setBigBackgroundColor(int color) {
        if (bigBackgroundPaint.getColor() != color) {
            bigBackgroundPaint.setColor(color);
            invalidateSelf();
        }
    }

    public int getBackgroundColor() {
        return backgroundPaint.getColor();
    }

    public void setTextColor(int color) {
        if (textPaint.getColor() != color) {
            textPaint.setColor(color);
            invalidateSelf();
        }
    }

    public int getTextColor() {
        return textPaint.getColor();
    }
}
