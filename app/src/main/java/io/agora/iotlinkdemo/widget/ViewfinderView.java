package io.agora.iotlinkdemo.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import io.agora.iotlinkdemo.R;

public final class ViewfinderView extends View {
    private static final long ANIMATION_DELAY = 1L;//刷新界面的时间
    private static final int DEFAULT_CORNER_WIDTH = 1;//四个绿色边角对应的宽度
    private static final int DEFAULT_CORNER_LENGTH = 30;//四个绿色边角对应的长度
    private static final int DEFAULT_MOVE_SPEED = 5;//中间那条线每次刷新移动的距离
    private static final int DEFAULT_TIP_TEXT_SIZE = 14;//默认字体大小
    private static final int DEFAULT_SCAN_RES_RECT_WIDTH = 7;//扫描线的粗细
    private static final int DEFAULT_TXT_PADDING_RECT = 10;//文本与取景框间距(默认值)
    private Paint mPaint = new Paint();//画笔对象的引用
    private int slideTop;//中间滑动线的最顶端位置
    private boolean tipAboveRect;//文本在上？
    private boolean isFirst;
    private int tipTextSize;//扫码文本字体大小
    private int scanLineColor;//扫描的线的颜色
    private int cornerColor;//角的颜色
    private int cornerWidth;//角的宽度
    private int cornerLength;//角的长
    private int textPaddingRect;//文本与取景框间距
    private int scanLineRes;//扫描线的资源id
    private int lineMoveSpeed;//扫描线移动速度
    private String tipText;//显示文本
    private String scanViewTitle;//扫码界面的Title
    private Context context;
    private static final String TAG = "ViewfinderView";
    private Point screenResolution;
    private Rect framingRect;

    /**
     * 当从XML布局里引用此控件的时候、会调用两个参数的构造函数
     *
     * @param context 上下文
     * @param attrs   自定义属性
     */
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e(TAG, "ViewfinderView: 进入：  ");
        this.context = context;
        this.initialAttrs(attrs);
    }

    /**
     * 读取XML里面的自定义属性
     *
     * @param attrs 自定义属性
     */
    private void initialAttrs(AttributeSet attrs) {
        TypedArray td = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);

        //----------------------------------提示文本--------------------------------------------
        tipText = td.getString(R.styleable.ViewfinderView_scan_tip_text);

        //----------------------------------文本是否处于扫描框的上方--------------------------------
        tipAboveRect = td.getBoolean(R.styleable.ViewfinderView_scan_text_above_rect, false);

        //----------------------------------提示文本与扫描框的距离---------------------------------
        textPaddingRect = td.getDimensionPixelSize(R.styleable.ViewfinderView_scan_text_padding_rect, DEFAULT_TXT_PADDING_RECT);

        //----------------------------------提示文本大小---------------------------------------
        tipTextSize = td.getDimensionPixelSize(R.styleable.ViewfinderView_scan_tip_text_size, DEFAULT_TIP_TEXT_SIZE);

        //----------------------------------角的长度---------------------------------------------
        cornerLength = td.getDimensionPixelSize(R.styleable.ViewfinderView_scan_rect_corner_length, DEFAULT_CORNER_LENGTH);

        //----------------------------------角的颜色---------------------------------------------
        cornerColor = td.getColor(R.styleable.ViewfinderView_scan_rect_corner_color, Color.GREEN);

        //----------------------------------扫描的线的颜色---------------------------------------------
        scanLineColor = td.getColor(R.styleable.ViewfinderView_scan_rect_corner_color, Color.GREEN);

        //----------------------------------角宽-----------------------------------------------
        cornerWidth = td.getDimensionPixelOffset(R.styleable.ViewfinderView_scan_rect_corner_width, DEFAULT_CORNER_WIDTH);

        //----------------------------------扫描线---------------------------------------------
        scanLineRes = td.getResourceId(R.styleable.ViewfinderView_scan_line_res, -1);

        //----------------------------------扫描界面的Title---------------------------------------------
        scanViewTitle = td.getString(R.styleable.ViewfinderView_scan_view_title_text);

        //----------------------------------扫描线移动速度--------------------------------------
        lineMoveSpeed = td.getInteger(R.styleable.ViewfinderView_scan_line_move_speed, DEFAULT_MOVE_SPEED);
        lineMoveSpeed = lineMoveSpeed > DEFAULT_MOVE_SPEED ? DEFAULT_MOVE_SPEED : lineMoveSpeed;

        td.recycle();
    }

    /**
     * 绘制扫描的界面
     *
     * @param canvas 画布
     */
    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
       /* Rect frame = CameraManager.get().getFramingRect();
        Log.e(TAG, "onDraw:   " );
        if (frame == null) {
            Log.e(TAG, "onDraw: frame == null  " );
            return;
        }*/
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        screenResolution = new Point(screenWidth, screenHeight);
        int mWidth = screenResolution.x * 2 / 3;
        int mHeight = screenResolution.x * 2 / 3;
        int leftOffset = (screenResolution.x - mWidth) >> 1;
        int topOffset = (screenResolution.y - mHeight) >> 2;
        framingRect = new Rect(leftOffset, topOffset, leftOffset + mWidth, topOffset + mHeight);
        //初始化中间线滑动的最上边和最下边
        if (!isFirst) {
            isFirst = true;
            slideTop = framingRect.top;
        }
        //获取屏幕的宽和高
        int width = getWidth();
        int height = getHeight();
        //画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        //扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        mPaint.setAlpha(0x40);
        canvas.drawRect(0, 0, width, framingRect.top, mPaint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom + 1, mPaint);
        canvas.drawRect(framingRect.right + 1, framingRect.top, width, framingRect.bottom + 1, mPaint);
        canvas.drawRect(0, framingRect.bottom + 1, width, height, mPaint);
        //画扫描框边上的角，总共8个部分
        mPaint.setAlpha(0xff);
        mPaint.setColor(cornerColor);
        canvas.drawRect(framingRect.left, framingRect.top, framingRect.left + cornerLength, framingRect.top + cornerWidth, mPaint);
        canvas.drawRect(framingRect.left, framingRect.top, framingRect.left + cornerWidth, framingRect.top + cornerLength, mPaint);
        canvas.drawRect(framingRect.right - cornerLength, framingRect.top, framingRect.right, framingRect.top + cornerWidth, mPaint);
        canvas.drawRect(framingRect.right - cornerWidth, framingRect.top, framingRect.right, framingRect.top + cornerLength, mPaint);
        canvas.drawRect(framingRect.left, framingRect.bottom - cornerWidth, framingRect.left + cornerLength, framingRect.bottom, mPaint);
        canvas.drawRect(framingRect.left, framingRect.bottom - cornerLength, framingRect.left + cornerWidth, framingRect.bottom, mPaint);
        canvas.drawRect(framingRect.right - cornerLength, framingRect.bottom - cornerWidth, framingRect.right, framingRect.bottom, mPaint);
        canvas.drawRect(framingRect.right - cornerWidth, framingRect.bottom - cornerLength, framingRect.right, framingRect.bottom, mPaint);
        //---------------绘制中间的线,每次刷新界面，中间的线往下移动SPEED_DISTANCE---------------//
        slideTop += lineMoveSpeed;
        if (slideTop + DEFAULT_SCAN_RES_RECT_WIDTH >= framingRect.bottom) {
            slideTop = framingRect.top;
        }
        Rect lineRect = new Rect();
        lineRect.left = framingRect.left;
        lineRect.right = framingRect.right;
        lineRect.top = slideTop;
        lineRect.bottom = slideTop + DEFAULT_SCAN_RES_RECT_WIDTH;
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), scanLineRes);
//        if (null == bitmap) {
//            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.player2);
//            bitmap = tintBitmap(bitmap, scanLineColor);
//        }
        Bitmap bitmap = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.bg_qr_scanning_line));
        canvas.drawBitmap(bitmap, null, lineRect, mPaint);
        //----------------------------画扫描框下面的字---------------------------------//
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(tipTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTypeface(Typeface.create(ViewfinderView.class.getSimpleName(), Typeface.BOLD));
        if (!TextUtils.isEmpty(tipText)) {
            if (tipAboveRect) {
                canvas.drawText(tipText, getWidth() >> 1, framingRect.top - textPaddingRect, mPaint);
            } else {
                Rect rect = new Rect();
                mPaint.getTextBounds(tipText, 0, tipText.length(), rect);
                canvas.drawText(tipText, getWidth() >> 1, framingRect.bottom + rect.height() + textPaddingRect, mPaint);
            }
        }
        //只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(ANIMATION_DELAY, framingRect.left, framingRect.top, framingRect.right, framingRect.bottom);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * 获取扫码界面要显示的Title
     *
     * @return 标题文本
     */
    public String getScanViewTitle() {
        return TextUtils.isEmpty(scanViewTitle) ? "扫描二维码" : scanViewTitle;
    }

    public void drawViewfinder() {
        invalidate();
    }

    /**
     * modify the bitmap's color.
     *
     * @param inBitmap  old bitmap.
     * @param tintColor the color what you would like modify.
     * @return bitmap after modified the color
     */
    public static Bitmap tintBitmap(Bitmap inBitmap, int tintColor) {
        if (inBitmap == null) return null;
        Bitmap outBitmap = Bitmap.createBitmap(inBitmap.getWidth(), inBitmap.getHeight(), inBitmap.getConfig());
        Canvas canvas = new Canvas(outBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(inBitmap, 0, 0, paint);
        return outBitmap;
    }

}