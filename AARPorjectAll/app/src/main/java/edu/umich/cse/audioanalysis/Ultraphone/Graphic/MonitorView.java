package edu.umich.cse.audioanalysis.Ultraphone.Graphic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import edu.umich.cse.audioanalysis.C;

/**
 * Created by eddyxd on 11/19/15.
 * 2016/05/21: add function to plot box
 */
public class MonitorView extends View{

    double gridOffset;
    Rect viewSize;

    ArrayList<LinkedList<Double>> lines;
    ArrayList<Paint> linePaints;
    Paint paintAxis, paintTextTick, paintBox;

    // internal states
    double xMin=0, xMax=0, yMin=0, yMax=0;
    double yStep, xStep; // how long to move for each point

    double boxXMin=0, boxXMax=0, boxYMin=0, boxYMax=0;
    boolean needToDrawBox = false;

    double MARGIN_RATIO_LEFT = 0.05;
    double MARGIN_RATIO_TOP = 0.1;
    double MARGIN_RATIO_RIGHT = 0.05;
    double MARGIN_RATIO_BOTTOM = 0.1;

    public MonitorView(Context context) {
        super(context);
        init();
    }

    public MonitorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MonitorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

        float density = getResources().getDisplayMetrics().density;

        gridOffset = 5*density;

        viewSize = new Rect(0,0,(int)(320*density),(int)(180*density)); // NOTE: this setting needs to compile with the layout setting
        setBackgroundColor(Color.LTGRAY);

        xMin = 0;
        xMax = 100;
        yMin = -1;
        yMax = 1;

        /*
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        */

        paintAxis = new Paint();
        paintAxis.setColor(Color.BLACK);
        paintAxis.setStrokeWidth(2);

        paintTextTick = new Paint();
        paintTextTick.setColor(Color.BLACK);
        paintTextTick.setTextSize(30);
        paintTextTick.setTextAlign(Paint.Align.CENTER);
        paintTextTick.setAntiAlias(true);

        lines = new ArrayList<>();
        linePaints = new ArrayList<>();


        paintBox = new Paint();
        paintBox.setColor(Color.YELLOW);

    }

    public void showLines(int lineCnt, double xMin, double xMax, double yMin, double yMax){
        updateRect(xMin, xMax, yMin, yMax);


        // pop out old lines
        lines.removeAll(null);

        // add new lines
        for(int i=0;i<lineCnt;i++){
            LinkedList<Double> q = new LinkedList<>();
            lines.add(q);
        }

        // add patins
        int[] lineColors = {Color.RED, Color.BLUE, Color.GREEN};
        int lineColorUndefined = Color.BLUE;
        for(int i=0;i<lineCnt;i++){
            Paint paint = new Paint();
            paint.setStrokeWidth(10);
            if(i<lineColors.length) {
                paint.setColor(lineColors[i]);
            } else {
                paint.setColor(lineColorUndefined);
            }
            linePaints.add(paint);
        }
    }

    public void addPoint(int lineIdx, double value){
        if(lineIdx < lines.size()){
            LinkedList<Double> line = lines.get(lineIdx);
            line.add(new Double(value));

            if(line.size() > xMax){
                line.poll();
            }

            this.invalidate();
        } else {
            Log.e(C.LOG_TAG, "[ERROR]: lineIdx > line size");
        }
    }

    public void updateRect(double xMin, double xMax, double yMin, double yMax){
        // update parameters
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        updateMarginAndPlotOffset();
    }

    float xOffsetLeft, yOffsetTop, xOffsetRight, yOffsetBottom;
    void updateMarginAndPlotOffset(){
        xOffsetLeft = (float)(viewSize.width()*MARGIN_RATIO_LEFT);
        yOffsetTop = (float)(viewSize.height()*MARGIN_RATIO_TOP);
        xOffsetRight = (float)(viewSize.width()*MARGIN_RATIO_RIGHT);
        yOffsetBottom = (float)(viewSize.height()*MARGIN_RATIO_BOTTOM);

        this.yStep = (viewSize.height()-yOffsetTop-yOffsetBottom)  / (yMax-yMin);
        this.xStep = (viewSize.width()-xOffsetLeft-xOffsetRight) / (xMax-xMin);
    }

    public void drawBox( double boxXMinIn, double boxXMaxIn, double boxYMinIn, double boxYMaxIn){
        boxXMin = boxXMinIn;
        boxXMax = boxXMaxIn;
        boxYMin = boxYMinIn;
        boxYMax = boxYMaxIn;
        needToDrawBox = true;
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        updateMarginAndPlotOffset();

        // 1. plot axis


        canvas.drawLine(xOffsetLeft, (viewSize.height() - yOffsetBottom), viewSize.width()-xOffsetRight, (viewSize.height() - yOffsetBottom), paintAxis); // plot x
        canvas.drawLine(xOffsetLeft, (viewSize.height() - yOffsetBottom), xOffsetLeft, yOffsetTop, paintAxis);


        canvas.drawText(String.format("%.0f", yMin), xOffsetLeft, (viewSize.height() - yOffsetBottom / 2), paintTextTick);
        canvas.drawText(String.format("%.2f", yMax), xOffsetLeft, (yOffsetTop / 2), paintTextTick);


        // 2. draw box if need
        if(needToDrawBox){
            float boxYTopToDraw = (float)(viewSize.height()-(boxYMax-yMin)*yStep-yOffsetBottom);
            float boxYBottomToDraw = (float)(viewSize.height()-(boxYMin-yMin)*yStep-yOffsetBottom);
            canvas.drawRect((float)(xOffsetLeft+boxXMin*xStep),boxYTopToDraw,(float)(xOffsetLeft+boxXMax*xStep),boxYBottomToDraw,paintBox);
        }


        // 3. plot lines
        for(int lineIdx=0; lineIdx < lines.size(); lineIdx++){
            float startX = (float)(xMin*xStep) + xOffsetLeft;
            float startY = (viewSize.height() - yOffsetBottom);

            LinkedList<Double> line = lines.get(lineIdx);
            for(int i=0;i< line.size();i++){
                Double d = line.get(i);

                float nowX = (float)(i*xStep) + xOffsetLeft;
                float nowY = (float)(viewSize.height()-(d.floatValue()-yMin)*yStep-yOffsetBottom);

                canvas.drawLine(startX, startY, nowX, nowY, linePaints.get(lineIdx));

                // update start
                startX = nowX;
                startY = nowY;
            }
        }
    }

}
