package io.agora.wayangdemo.widget;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 使用与正常imageview没有任何区别，而这个imageview支持的是双指缩放，把它放在任何布局中都可以，已经解决好了嵌套滑动等手势冲突的问题
 * 需要配合 MakeUpZoomImage 使用
 *
 * @see MakeUpZoomImage
 */
public class ZoomImageView extends AppCompatImageView {
    private static final String TAG = "ZoomImageView";
    private double lastD;
    private Point lastCenter;
    private int pCont;

    public ZoomImageView(Context context) {
        super(context);
        setClickable(true);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pCont = 1;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                pCont++;
                if (pCont >= 2) {
                    setElevation(100);
                    bringToFront();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    lastD = getDistance(event);
                    lastCenter = getCurrentPoint(event);
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                pCont--;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    double moved = getDistance(event) - lastD;
                    double viewSize = Math.sqrt(getWidth() * getWidth() + getHeight() * getHeight());
                    double scale = (moved / viewSize);
                    Log.d(TAG, scale + "   " + lastD);
                    setVisibility(INVISIBLE);
                    Point point = getCurrentPoint(event);
                    float tx = point.x - lastCenter.x;
                    float ty = point.y - lastCenter.y;
                    MakeUpZoomImage.get().update(this, getDrawable(), scale + 1, tx, ty);
                    return true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                MakeUpZoomImage.get().remove();
                setVisibility(VISIBLE);
                getParent().requestDisallowInterceptTouchEvent(false);
                pCont = 0;
                setScaleX(1);
                setScaleY(1);
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private Point getCurrentPoint(MotionEvent event) {
        float x0 = event.getX(0);
        float y0 = event.getY(0);
        float x1 = event.getX(1);
        float y1 = event.getY(1);
        Point point = new Point(((int) (x1 + x0) / 2), ((int) (y1 + y0) / 2));
        return point;
    }

    private double getDistance(MotionEvent event) {
        float x0 = event.getX(0);
        float y0 = event.getY(0);
        float x1 = event.getX(1);
        float y1 = event.getY(1);

        return Math.sqrt((
                (y1 - y0) * (y1 - y0))
                + ((x1 - x0) * (x1 - x0))
        );
    }
}