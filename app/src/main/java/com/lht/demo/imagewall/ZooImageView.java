package com.lht.demo.imagewall;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by lht on 2017/5/2.
 */

public class ZooImageView extends View {

    /**
     * 初始化状态变量
     */
    public static final int STATUS_INIT = 0X111;

    /**
     * 图片方法状态变量
     */
    public static final int STATUS_ZOOM_OUT = 0X112;
    /**
     * 图片缩小状态变量
     */
    public static final int STATUS_ZOOM_IN = 0X113;
    /**
     * 图片拖动状态变量
     */
    public static final int STATUS_MOVE = 0X114;

    /**
     * 用于对图片进行移动和缩放变换的矩阵
     */
    private Matrix matrix = new Matrix();

    private Bitmap mBitmap;

    /**
     * 记录当前状态的值
     */
    private int currentStatus;

    /**
     * 控件的宽
     */
    private int width;

    /**
     * 控件的高
     */
    private int height;

    /**
     * 两根手指同时放在屏幕上时，中心点的横坐标值
     */
    private float centerPointX;
    /**
     * 两根手指同时放在屏幕上时，中心点的纵坐标值
     */
    private float centerPointY;

    /**
     * 记录当前图片的宽度，图片被缩放时，该值会跟着一起变
     */
    private float currentBitmapWidth;
    /**
     * 记录当前图片的高度，图片被缩放时，该值会跟着一起变
     */
    private float currentBitmapheight;

    /**
     * 记录上次手指移动时的横坐标
     */
    private float lastXMove = -1;
    /**
     * 记录上次手指移动时的纵坐标
     */
    private float lastYMove = -1;

    /**
     * 记录手指在横坐标方向上的移动距离
     */
    private float movedDistanceX;
    /**
     * 记录手指在纵坐标方向上的移动距离
     */
    private float movedDistanceY;

    /**
     * 记录图片在矩阵上的横向偏移值
     */
    private float totalTranslateX;

    /**
     * 记录图片在矩阵上的纵向偏移值
     */
    private float totalTranslateY;

    /**
     * 记录图片在矩阵上的总缩放比例
     */
    private float totalRatio;

    /**
     * 记录手指移动的距离所造成的缩放比例
     */
    private float scaledRatio;

    /**
     * 记录图片初始化时的缩放比例
     */
    private float initRatio;

    /**
     * 记录上次两指之间的距离
     */
    private double lastFingerDis;


    public ZooImageView(Context context) {
        super(context);
    }

    public ZooImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        currentStatus = STATUS_INIT;
    }

    public ZooImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 将待展示的图片设置进来
     *
     * @param mBitmap 待展示的图片对象
     */
    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public void setmBitmap(Resources resources, int id) {
        this.mBitmap=BitmapFactory.decodeResource(resources,id);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            width = getWidth();
            height = getHeight();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    // 当有两个手指按在屏幕上时，计算两指之间的距离
                    lastFingerDis = distanceBetweenFingers(event);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    //只有单指按在屏幕上时，为拖动状态
                    float xMove = event.getX();
                    float yMove = event.getY();
                    if (lastXMove == -1 && lastYMove == -1) {
                        lastXMove = xMove;
                        lastYMove = yMove;
                    }
                    currentStatus = STATUS_MOVE;
                    movedDistanceX = xMove - lastXMove;
                    movedDistanceY = yMove - lastYMove;
                    //进行边界检查，不允许将图片脱出边界
                    if (totalTranslateX + movedDistanceX > 0) {
                        movedDistanceX = 0;
                    } else if (width - (totalTranslateX + movedDistanceX) > currentBitmapWidth) {
                        movedDistanceX = 0;
                    }

                    if (totalTranslateY + movedDistanceY > 0) {
                        movedDistanceY = 0;
                    } else if (height - (totalTranslateY + movedDistanceY) > currentBitmapheight) {
                        movedDistanceY = 0;
                    }
                    //调用onDraw()方法绘制图片
                    invalidate();
                    lastXMove = xMove;
                    lastYMove = yMove;
                } else if (event.getPointerCount() == 2) {
                    //有两根手指在屏幕上移动时，为缩放状态
                    centerPointBetweenFingers(event);
                    double fingerDis = distanceBetweenFingers(event);
                    if (fingerDis > lastFingerDis) {
                        currentStatus = STATUS_ZOOM_OUT;
                    } else {
                        currentStatus = STATUS_ZOOM_IN;
                    }
                    //进行缩放倍数检查，最大只允许将图片放大4倍，最小可以缩小到初始化比例
                    if ((currentStatus == STATUS_ZOOM_OUT && totalRatio < 4 * initRatio) || (currentStatus == STATUS_ZOOM_IN && totalRatio > initRatio)) {
                        scaledRatio = (float) (fingerDis / lastFingerDis);
                        totalRatio = totalRatio * scaledRatio;
                        if (totalRatio > 4 * initRatio) {
                            totalRatio = 4 * initRatio;
                        } else if (totalRatio < initRatio) {
                            totalRatio = initRatio;
                        }
                        //调用onDraw()方法绘制图片
                        invalidate();
                        lastFingerDis = fingerDis;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2) {
                    //手指离开屏幕时将临时值还原
                    lastXMove = -1;
                    lastYMove = -1;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     *
     * 根据currentStatus的值来决定对图片进行什么样的绘制操作
     *
     *
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (currentStatus) {
            case STATUS_ZOOM_OUT:
            case STATUS_ZOOM_IN:
                zoom(canvas);
                break;
            case STATUS_MOVE:
                move(canvas);
                break;
            case STATUS_INIT:
                initBitmap(canvas);
            default:
                canvas.drawBitmap(mBitmap, matrix, null);
                break;
        }
    }

    /**
     *
     * 对图片进行缩放处理
     *
     * @param canvas
     */
    private void zoom(Canvas canvas) {
        matrix.reset();
        //将图片按总比例缩放
        matrix.postScale(totalRatio, totalRatio);
        float scaledWidth = mBitmap.getWidth() * totalRatio;
        float scaledHeight = mBitmap.getHeight() * totalRatio;
        float translateX = 0f;
        float translateY = 0f;
        //如果当前图片宽度小于屏幕宽度，则按屏幕中心的横坐标进行水平缩放，否则按两指的中心点的横坐标进行水平缩放
        if (currentBitmapWidth < width) {
            translateX = (width - scaledWidth) / 2f;
        } else {
            translateX = totalTranslateX * scaledRatio + centerPointX * (1 - scaledRatio);
            //进行边界检查，保证图片缩放后在水平方向上不会偏移出屏幕
            if (translateX > 0) {
                translateX = 0;
            } else if (width - translateX > scaledWidth) {
                translateX = width - scaledWidth;
            }
        }
        //如果当前图片高度小于屏幕高度，则按屏幕中心的纵坐标进行垂直缩放。否则按两指的中心点的纵坐标进行垂直缩放
        if (currentBitmapheight < height) {
            translateY = (height - scaledHeight) / 2f;
        } else {
            translateY = totalTranslateY * scaledRatio + centerPointY * (1 - scaledRatio);
            //进行边界检查，保证图片缩放后在垂直方向上不会偏移屏幕
            if (translateY > 0) {
                translateY = 0;
            } else if (height - translateY > scaledHeight) {
                translateY = height - scaledHeight;
            }
        }
        //缩放后对图片进行偏移，以保证缩放后中心点位置不变
        matrix.postTranslate(translateX, translateY);
        totalTranslateX = translateX;
        totalTranslateY = translateY;
        currentBitmapWidth = scaledWidth;
        currentBitmapheight = scaledHeight;
        canvas.drawBitmap(mBitmap, matrix, null);
    }

    /**
     * 对图片进行平移处理
     *
     * @param canvas
     */
    private void move(Canvas canvas) {
        matrix.reset();
        //根据手指移动的距离计算出总偏移量
        float translateX = totalTranslateX + movedDistanceX;
        float translateY = totalTranslateY + movedDistanceY;
        //先按照已有的缩放比例对图片进行缩放
        matrix.postScale(totalRatio, totalRatio);
        //再根据移动距离进行偏移
        matrix.postTranslate(translateX, translateY);
        totalTranslateX = translateX;
        totalTranslateY = translateY;
        canvas.drawBitmap(mBitmap, matrix, null);
    }


    private void initBitmap(Canvas canvas) {
        if (mBitmap != null) {
            matrix.reset();
            int bitmapWidth = mBitmap.getWidth();
            int bitmapheight = mBitmap.getHeight();
            if (bitmapWidth > width || bitmapheight > height) {
                if (bitmapWidth - width > bitmapheight - height) {
                    //当图片宽度大于屏幕宽度时，将图片等比例压缩，使它可以完全显示出来
                    float ratio = width / (bitmapWidth * 1.0f);
                    matrix.postScale(ratio, ratio);
                    float translateY = (height - (bitmapheight * ratio)) / 2f;
                    //在纵坐标方向上进行偏移，以保证图片居中显示
                    matrix.postTranslate(0, translateY);
                    totalTranslateY = translateY;
                    //这个结果是？
                    totalRatio = initRatio = ratio;
                } else {
                    //当图片高度大于屏幕高度时，将图片等比例压缩，使它可以完全显示出来
                    float ratio = height / (bitmapheight * 1.0f);
                    matrix.postScale(ratio, ratio);
                    float translateX = (width - (bitmapWidth * ratio)) / 2f;
                    //在横坐标方向上进行偏移，以保证图片居中显示
                    matrix.postTranslate(translateX, 0);
                    totalTranslateX = translateX;
                    totalRatio = initRatio = ratio;
                }
                currentBitmapheight = bitmapheight * initRatio;
                currentBitmapWidth = bitmapWidth * initRatio;
            } else {
                //当图片宽高都小于屏幕宽高时，直接让图片居中显示
                float translateX = (width - mBitmap.getWidth()) / 2f;
                float translateY = (height - mBitmap.getHeight()) / 2f;
                matrix.postTranslate(translateX, translateY);
                totalTranslateX = translateX;
                totalTranslateY = translateY;
                totalRatio = initRatio = 1f;
                currentBitmapWidth = bitmapWidth;
                currentBitmapheight = bitmapheight;
            }
            canvas.drawBitmap(mBitmap, matrix, null);
        }
    }







    /**
     * @param event
     * @return 两手指之间的距离
     */
    private double distanceBetweenFingers(MotionEvent event) {
        float disX = Math.abs(event.getX(0) - event.getX(1));
        float disY = Math.abs(event.getY(0) - event.getY(1));
        return Math.sqrt(disX * disX + disY * disY);
    }


    private void centerPointBetweenFingers(MotionEvent event) {
        float xPoint0 = event.getX(0);
        float yPoint0 = event.getY(0);
        float xPoint1 = event.getX(1);
        float yPoint1 = event.getY(1);
        centerPointX = (xPoint0 + xPoint1) / 2;
        centerPointY = (yPoint0 + yPoint1) / 2;
    }
}
