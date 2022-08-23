package com.cloud.duolib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cloud.duolib.R;

public class HorizontalDownloadProgressBar extends ProgressBar {

    public static final int DOWNLOADING = 0x001;//下载
    public static final int PAUSE = 0x002;//下载暂停
    public static final int FINISH = 0x003;//下载完成
    public static final int START = 0x004;//下载开始
    private int mState = START;//默认状态时开始
    private final int colorWhite;
    private int colorProgress;//进度条默认背景颜色
    private int colorBackground;
    //进度条文字默认颜色
    private String mProgressInfo;
    //进度条最小宽度
    private Paint mPaint;
    private Path mPath;

    private void setProgressState(int state) {
        mState = state;
        postInvalidate();
    }

    public void setStyleGreen(Context context) {
        colorProgress = ContextCompat.getColor(context, R.color.green);
        colorBackground = ContextCompat.getColor(context, R.color.gray3);
        setClickable(false);
    }

    public void updateProgress(int curProgress, @NonNull String curInfo) {
        this.setProgress(curProgress);
        this.mProgressInfo = curInfo;
        switch (curProgress) {
            case 0: {
                setProgressState(START);
                setClickable(false);
                break;
            }
            case 99: {
                this.setProgress(100);
                setProgressState(PAUSE);
                break;
            }
            case 100: {
                setProgressState(FINISH);
                setClickable(true);
                break;
            }
            default: {
                setProgressState(DOWNLOADING);
                break;
            }
        }
    }

    public HorizontalDownloadProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        colorWhite = ContextCompat.getColor(context, R.color.white);
        colorProgress = ContextCompat.getColor(context, R.color.blue);
        colorBackground = ContextCompat.getColor(context, R.color.duo_media_blue);
        initProgressBar();
    }

    //初始化画笔
    private void initProgressBar() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPath = new Path();
        mPaint.setTextSize(sp2px());
        mPaint.setStrokeWidth(dp2px());
        setClickable(false);
        setMax(100);
    }

    //进度条边框宽度
    private int dp2px() {
        float pxValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        return (int) (pxValue + 0.5f);
    }

    //进度条字体大小
    private int sp2px() {
        float pxValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        return (int) (pxValue + 0.5f);
    }

    //此处主要测量文字的高度和ProgressBar的高度之间的最大值
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //比较一下文字的高度和height的大小，谁大用谁的
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), heightMeasureSpec);
    }

    //分别绘制四种状态
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        switch (mState) {
            case DOWNLOADING:
                drawProgressOnDownload(canvas);
                break;
            case PAUSE:
                drawProgressOnPause(canvas);
                break;
            case FINISH:
                drawProgressOnFinished(canvas);
                break;
            case START:
                drawProgressOnStart(canvas);
                break;
            default:
                break;
        }
    }

    private void drawProgressOnDownload(Canvas canvas) {
        //背景填充
        drawProgressRectBg(canvas, false);
        drawProgressRect(canvas);
        //白色进度
        drawProgressText(canvas, this.getProgress() + "%", colorWhite);
    }

    private void drawProgressOnPause(Canvas canvas) {
        //背景填充
        drawProgressRectBg(canvas, false);
        drawProgressRect(canvas);
        //白色信息
        drawProgressText(canvas, mProgressInfo, colorWhite);
    }

    private void drawProgressOnFinished(Canvas canvas) {
        drawProgressOnStart(canvas);
    }

    private void drawProgressOnStart(Canvas canvas) {
        //背景边框
        drawProgressRectBg(canvas, true);
        //蓝色信息
        drawProgressText(canvas, mProgressInfo, colorProgress);
    }

    //  绘制背景矩形
    private void drawProgressRectBg(Canvas canvas, Boolean outline) {
        int width = getWidth() - getPaddingRight() - getPaddingLeft();
        int height = getHeight() - getPaddingBottom() - getPaddingTop();
        if (outline) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(colorProgress);
        } else {
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(colorBackground);
        }
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        //绘制一个路径
        mPath.reset();
        float pw = mPaint.getStrokeWidth();//画笔的宽度
        mPath.moveTo(height / 2.0f - pw / 2, height - pw / 2);//从这个点开始
        mPath.arcTo(new RectF(pw / 2, pw / 2, height * 1.0f - pw / 2, height * 1.0f - pw / 2), 90, 180);//添加一个左边弧形
        mPath.lineTo(width - height / 2.0f - pw / 2, pw / 2);//连接这个点
        mPath.arcTo(new RectF(width - height + pw / 2, pw / 2, width - pw / 2, height - pw / 2), -90, 180);//添加一个右边弧形
        mPath.lineTo(width - height / 2.0f - pw / 2, height - pw / 2);//连接这个点
        mPath.lineTo(height / 2.0f - pw / 2, height - pw / 2);//连接这个点
        //关闭
        mPath.close();
        canvas.drawPath(mPath, mPaint);
        canvas.restore();
    }

    //  绘制矩形的下载进度
    private void drawProgressRect(Canvas canvas) {
        int width = getWidth() - getPaddingRight() - getPaddingLeft();//考虑左右边距
        int height = getHeight() - getPaddingBottom() - getPaddingTop();//考虑上下边距
        float radio = getProgress() * 1.0f / getMax();
        int progress = (int) (width * radio);
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mPath.reset();
        //绘制一个路径
        mPath.moveTo(height / 2.0f, height);
        mPath.arcTo(new RectF(0, 0, height * 1.0f, height * 1.0f), 90, 180);
        mPath.lineTo(width - height / 2.0f, 0);
        mPath.arcTo(new RectF(width - height, 0, width, height), -90, 180);
        mPath.lineTo(width - height / 2.0f, height);
        mPath.lineTo(height / 2.0f, height);
        mPath.close();
        //绘制进度
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(colorProgress);
        canvas.clipPath(mPath);
        mPath.reset();
        mPath.addRect(new RectF(0, 0, progress, height), Path.Direction.CCW);
        canvas.clipPath(mPath, Region.Op.INTERSECT);
        canvas.drawColor(colorProgress);
        canvas.restore();
    }

    //  绘制进度中的文字
    private void drawProgressText(Canvas canvas, String text, int color) {
        int width = getWidth() - getPaddingRight() - getPaddingLeft();
        int height = getHeight() - getPaddingBottom() - getPaddingTop();
        //绘制文字
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mPaint.setColor(color);
        if (text == null) {
            text = this.getProgress() + "%";
        }
        int textWidth = (int) mPaint.measureText(text);
        int textHeight = (int) (mPaint.descent() + mPaint.ascent());
        canvas.drawText(text, width / 2.0f - textWidth / 2.0f, height / 2.0f - textHeight / 2.0f, mPaint);
        canvas.restore();
    }
}