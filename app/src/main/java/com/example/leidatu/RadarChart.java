package com.example.leidatu;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class RadarChart extends View {
    private final Context mContext;
    private Path mRdPath;

    private int mCornerCount = 5;//角个数

    private int mRingCount = 5; //雷达圆环个数
    private int pointImg = R.drawable.point_icon;
    //中心X,Y点坐标
    private float mCenterX, mCenterY;
    //半径
    private float mRadius = 0.0f;
    //标签文字
    private List<String> mlables;
    //每个标签点的百分比
    private LinkedList<Double> dataSeries;
    private LinkedList<Integer> dataValue;
    //雷达图每个点的X,Y坐标
    private float[][] mArrayDotX = null, mArrayDotY = null;
    //雷达图外围标签文字X,Y坐标
    private float[][] mArrayLabelX = null, mArrayLabelY = null;
    //每个圆环半径
    private Float[] mArrayRadius = null;
    private Paint mPaintLine;//雷达线画笔
    private Paint mPaintLabel;//每个点的标签文字画笔
    private Paint mPaintLabel2;//每个点的标签2文字画笔
    private Paint mPaintArea;//阴影区域画笔
    //每个标签节点归属的圆心角度
    private Float[] mArrayLabelAgent = null;


    public RadarChart(Context context) {
        this(context, null);
    }

    public RadarChart(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView();
    }

    public void setData(ArrayList<ScoreItem> list) {
        mlables.clear();
        list.forEach(item -> {
            mlables.add(item.getName());
            dataValue.add(item.getValue());
            dataSeries.add((item.getValue() * 0.8 / 100) * 100);
        });
        postInvalidate();
    }

    private void initView() {
        mRdPath = new Path();
        mlables = new LinkedList<>();
        mlables.add("沟通能力");
        mlables.add("专业技能");
        mlables.add("理论知识");
        mlables.add("创新能力");
        mlables.add("协调能力");
        dataValue = new LinkedList<Integer>();
        dataValue.add(55);
        dataValue.add(42);
        dataValue.add(5);
        dataValue.add(58);
        dataValue.add(37);
        dataSeries = new LinkedList<Double>();
        dataValue.forEach(item -> {
            dataSeries.add((item * 0.8 / 100) * 100);
        });
        //蜘蛛网和各个轴线的画笔
        mPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintLine.setColor(Color.parseColor("#b0c2ff"));
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setStrokeWidth(dip2px(mContext, 1));
        //标签文字画笔
        mPaintLabel = new Paint();
        mPaintLabel.setColor(Color.parseColor("#666666"));
        mPaintLabel.setTextSize(dip2px(mContext, 14));
        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setTextAlign(Paint.Align.CENTER);
        //标签2文字画笔
        mPaintLabel2 = new Paint();
        mPaintLabel2.setColor(Color.parseColor("#3DC17D"));
        mPaintLabel2.setTextSize(dip2px(mContext, 12));
        mPaintLabel2.setAntiAlias(true);
        mPaintLabel2.setTextAlign(Paint.Align.CENTER);
        // 设置文字加粗
        mPaintLabel2.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD)); // 设置文字为加粗
        mPaintLabel2.setFakeBoldText(true); // 设置文字为加粗
        //阴影区域画笔
        mPaintArea = new Paint();
        mPaintArea.setColor(Color.parseColor("#8038E28A"));
        mPaintArea.setAntiAlias(true);
        mPaintArea.setStrokeWidth(5);
        mPaintArea.setAlpha(100);
    }


    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        mCenterX = Math.abs(div(width, 2f));
        mCenterY = Math.abs(div(height, 2f));
        mRadius = Math.min(div(width, 2f), div(height, 2f)) * 0.6f;
        super.onSizeChanged(width, height, oldwidth, oldheight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //计算各个定点的坐标
        calcAllPoints();
        //绘制蜘蛛网
        renderGridLinesRadar(canvas);
        //绘制圆心到定点的轴线
        renderAxisLines(canvas);
        //画标题
        renderAxisLabels(canvas);
        //画填充区域
        renderDataArea(canvas);
    }


    private void calcAllPoints() {
        //一条轴线上的点总数
        int dataAxisTickCount = mRingCount + 1;
        //扇形每个角的角度,
        float pAngle = div(360f, mCornerCount); //   72f;
        //270为中轴线所处圆心角
        float initOffsetAgent = sub(270f, pAngle);
        //当前圆心角偏移量
        float offsetAgent = 0.0f;
        //初始化雷达图各个点坐标和外围标签文字坐标
        mArrayDotX = new float[dataAxisTickCount][mCornerCount];
        mArrayDotY = new float[dataAxisTickCount][mCornerCount];
        mArrayLabelX = new float[dataAxisTickCount][mCornerCount];
        mArrayLabelY = new float[dataAxisTickCount][mCornerCount];
        //保存每个环数的半径长度
        mArrayRadius = new Float[dataAxisTickCount];
        //由总环数和半径长度算出平均刻度值
        float avgRadius = div(mRadius, mRingCount);
        mArrayLabelAgent = new Float[mCornerCount];
        float currAgent = 0.0f;
        for (int i = 0; i < mRingCount + 1; i++) //数据轴(从圆心点开始)
        {
            //平均刻度值算出每个圆环的半径
            mArrayRadius[i] = avgRadius * i;
            for (int j = 0; j < mCornerCount; j++) {
                offsetAgent = add(initOffsetAgent, pAngle * j);
                currAgent = add(offsetAgent, pAngle);
                //计算位置
                if (Float.compare(0.f, mArrayRadius[i]) == 0) {
                    mArrayDotX[i][j] = mCenterX;
                    mArrayDotY[i][j] = mCenterY;
                } else {
                    //点的位置
                    mArrayDotX[i][j] = calcArcEndPointXY(mCenterX, mCenterY, mArrayRadius[i], currAgent).x;
                    mArrayDotY[i][j] = calcArcEndPointXY(mCenterX, mCenterY, mArrayRadius[i], currAgent).y;
                }
                //记下每个标签对应的圆心角
                if (0 == i) mArrayLabelAgent[j] = currAgent;
            }
        }
    }


    //绘制蜘蛛网
    private void renderGridLinesRadar(Canvas canvas) {
        for (int i = 0; i < mRingCount + 1; i++) {
            if (i == mRingCount) {
                mPaintLine.setColor(Color.parseColor("#00000000")); // 最外层蜘蛛网线设为透明
                mPaintLine.setStrokeWidth(0); // 最外层蜘蛛网线宽度设为0
            } else if (i == mRingCount - 1) {
                mPaintLine.setColor(Color.parseColor("#293DC17D")); // 倒数第二层蜘蛛网线的颜色
                mPaintLine.setStrokeWidth(dip2px(mContext, 3)); // 倒数第二层蜘蛛网线的宽度
            } else {
                mPaintLine.setColor(Color.parseColor("#E9F4EC")); // 其他层蜘蛛网线的颜色
                mPaintLine.setStrokeWidth(dip2px(mContext, 1)); // 其他层蜘蛛网线的宽度
            }

            mRdPath.reset();
            for (int j = 0; j < mCornerCount; j++) {
                if (j == 0) {
                    mRdPath.moveTo(mArrayDotX[i][j], mArrayDotY[i][j]);
                } else {
                    mRdPath.lineTo(mArrayDotX[i][j], mArrayDotY[i][j]);
                }
            }
            mRdPath.close();
            canvas.drawPath(mRdPath, mPaintLine);
        }
    }


    //绘制各个方向上的轴线
    private void renderAxisLines(Canvas canvas) {
        mPaintLine.setColor(Color.parseColor("#293DC17D")); // 其他层蜘蛛网线的颜色
        mPaintLine.setStrokeWidth(dip2px(mContext, 1.5f)); // 其他层蜘蛛网线的宽度
        Paint dashedLinePaint = new Paint(mPaintLine); // 复制雷达线画笔属性
        dashedLinePaint.setPathEffect(new DashPathEffect(new float[]{10f, 10f}, 0f)); // 设置虚线效果
        for (int j = 0; j < mCornerCount; j++) {
            float startX = mCenterX;
            float startY = mCenterY;
            float endX = mArrayDotX[mRingCount][j];
            float endY = mArrayDotY[mRingCount][j];
            canvas.drawLine(mCenterX, mCenterY, mArrayDotX[mRingCount][j], mArrayDotY[mRingCount][j], dashedLinePaint); // 使用虚线画笔绘制线段

            // 绘制图片
            int imgSize = 40; // 图片尺寸
            Bitmap pointBitmap = BitmapFactory.decodeResource(getResources(), pointImg);
            int left = (int) (endX - imgSize / 2);
            int top = (int) (endY - imgSize / 2);
            int right = (int) (endX + imgSize / 2);
            int bottom = (int) (endY + imgSize / 2);
            Rect destRect = new Rect(left, top, right, bottom);
            canvas.drawBitmap(pointBitmap, null, destRect, null);
        }
    }


    //绘制最外围的标签
    private void renderAxisLabels(Canvas canvas) {
        for (int j = 0; j < mlables.size(); j++) {
            float xOffset, yOffset;
            if (mArrayDotX[mRingCount][j] > mCenterX && mArrayDotY[mRingCount][j] < mCenterY) { //右上
                mArrayLabelX[mRingCount][j] = mArrayDotX[mRingCount][j] + dip2px(mContext, 30);
                mArrayLabelY[mRingCount][j] = mArrayDotY[mRingCount][j] - dip2px(mContext, 8);
                xOffset = 0f;
                yOffset = 0f;
            } else if (mArrayDotX[mRingCount][j] > mCenterX && mArrayDotY[mRingCount][j] > mCenterY) {//又下
                mArrayLabelX[mRingCount][j] = mArrayDotX[mRingCount][j] + dip2px(mContext, 16);
                mArrayLabelY[mRingCount][j] = mArrayDotY[mRingCount][j] + dip2px(mContext, 26);
                xOffset = 0f;
                yOffset = 0f;
            } else if (mArrayDotX[mRingCount][j] <= mCenterX && mArrayDotY[mRingCount][j] > mCenterY) {//左下
                mArrayLabelX[mRingCount][j] = mArrayDotX[mRingCount][j] - dip2px(mContext, 16);
                mArrayLabelY[mRingCount][j] = mArrayDotY[mRingCount][j] + dip2px(mContext, 26);
                xOffset = 0f;
                yOffset = 0f;
            } else if (mArrayDotX[mRingCount][j] < mCenterX && mArrayDotY[mRingCount][j] < mCenterY) {//左上
                mArrayLabelX[mRingCount][j] = mArrayDotX[mRingCount][j] - dip2px(mContext, 30);
                mArrayLabelY[mRingCount][j] = mArrayDotY[mRingCount][j] - dip2px(mContext, 8);
                xOffset = 0f;
                yOffset = 0f;
            } else { //上
                mArrayLabelX[mRingCount][j] = mArrayDotX[mRingCount][j];
                mArrayLabelY[mRingCount][j] = mArrayDotY[mRingCount][j] - dip2px(mContext, 26);
                xOffset = 0f;
                yOffset = 0f;
            }
            canvas.drawText(mlables.get(j), mArrayLabelX[mRingCount][j], mArrayLabelY[mRingCount][j], mPaintLabel);
            // 绘制描述得分的文字
            String description = dataValue.get(j) + " 分";
            canvas.drawText(description, mArrayLabelX[mRingCount][j] + xOffset, mArrayLabelY[mRingCount][j] + yOffset + dip2px(mContext, 16), mPaintLabel2);

        }
    }


    /**
     * 绘制数据区网络
     *
     * @param canvas 画布
     */
    private void renderDataArea(Canvas canvas) {

        int dataSize = dataSeries.size();
        Float[] arrayDataX = new Float[dataSize];
        Float[] arrayDataY = new Float[dataSize];
        int i = 0;
        for (Double data : dataSeries) {
            if (Double.compare(data, 0.d) == 0) {
                arrayDataX[i] = mCenterX;
                arrayDataY[i] = mCenterY;
                i++; //标签
                continue;
            }
            float curRadius = (float) (mRadius * data / 100);
            //计算位置

            arrayDataX[i] = calcArcEndPointXY(mCenterX, mCenterY, curRadius, mArrayLabelAgent[i]).x;
            arrayDataY[i] = calcArcEndPointXY(mCenterX, mCenterY, curRadius, mArrayLabelAgent[i]).y;
            i++; //标签
        }

        float initX = 0.0f, initY = 0.0f;
        mRdPath.reset();
        for (int p = 0; p < arrayDataX.length; p++) {
            if (0 == p) {
                initX = arrayDataX[p];
                initY = arrayDataY[p];
                mRdPath.moveTo(initX, initY);
            } else {
                mRdPath.lineTo(arrayDataX[p], arrayDataY[p]);
            }
        }
        //收尾
        mRdPath.lineTo(initX, initY);
        mRdPath.close();
        canvas.drawPath(mRdPath, mPaintArea);
    }


    //设置标签
    public void setLableList(List<String> lableList) {
        this.mlables = lableList;
        postInvalidate();
    }

    //设置百分比
    public void setDataList(LinkedList<Double> dataList) {
        this.dataSeries = dataList;
        postInvalidate();
    }


    //依圆心坐标，半径，扇形角度，计算出扇形终射线与圆弧交叉点的xy坐标
    public PointF calcArcEndPointXY(float cirX, float cirY, float radius, float cirAngle) {
        PointF mPointF = new PointF();
        if (Float.compare(cirAngle, 0.0f) == 0 || Float.compare(radius, 0.0f) == 0) {
            return mPointF;
        }
        //将角度转换为弧度
        float arcAngle = (float) (Math.PI * div(cirAngle, 180.0f));
        if (Float.compare(arcAngle, 0.0f) == -1)
            mPointF.x = mPointF.y = 0.0f;
        if (Float.compare(cirAngle, 90.0f) == -1) {
            mPointF.x = add(cirX, (float) Math.cos(arcAngle) * radius);
            mPointF.y = add(cirY, (float) Math.sin(arcAngle) * radius);
        } else if (Float.compare(cirAngle, 90.0f) == 0) {
            mPointF.x = cirX;
            mPointF.y = add(cirY, radius);
        } else if (Float.compare(cirAngle, 90.0f) == 1 && Float.compare(cirAngle, 180.0f) == -1) {
            arcAngle = (float) (Math.PI * (sub(180f, cirAngle)) / 180.0f);
            mPointF.x = sub(cirX, (float) (Math.cos(arcAngle) * radius));
            mPointF.y = add(cirY, (float) (Math.sin(arcAngle) * radius));
        } else if (Float.compare(cirAngle, 180.0f) == 0) {
            mPointF.x = cirX - radius;
            mPointF.y = cirY;
        } else if (Float.compare(cirAngle, 180.0f) == 1 && Float.compare(cirAngle, 270.0f) == -1) {
            arcAngle = (float) (Math.PI * (sub(cirAngle, 180.0f)) / 180.0f);
            mPointF.x = sub(cirX, (float) (Math.cos(arcAngle) * radius));
            mPointF.y = sub(cirY, (float) (Math.sin(arcAngle) * radius));
        } else if (Float.compare(cirAngle, 270.0f) == 0) {
            mPointF.x = cirX;
            mPointF.y = sub(cirY, radius);
        } else {
            arcAngle = (float) (Math.PI * (sub(360.0f, cirAngle)) / 180.0f);
            mPointF.x = add(cirX, (float) (Math.cos(arcAngle) * radius));
            mPointF.y = sub(cirY, (float) (Math.sin(arcAngle) * radius));
        }
        return mPointF;
    }

    public float div(float v1, float v2) {
        BigDecimal bgNum1 = new BigDecimal(Float.toString(v1));
        BigDecimal bgNum2 = new BigDecimal(Float.toString(v2));
        return bgNum1.divide(bgNum2, 5, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    public float add(double v1, double v2) {
        BigDecimal bgNum1 = new BigDecimal(Double.toString(v1));
        BigDecimal bgNum2 = new BigDecimal(Double.toString(v2));
        return bgNum1.add(bgNum2).floatValue();
    }

    public float sub(float v1, float v2) {
        BigDecimal bgNum1 = new BigDecimal(Float.toString(v1));
        BigDecimal bgNum2 = new BigDecimal(Float.toString(v2));
        return bgNum1.subtract(bgNum2).floatValue();
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int dip2px(Context context, int dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}