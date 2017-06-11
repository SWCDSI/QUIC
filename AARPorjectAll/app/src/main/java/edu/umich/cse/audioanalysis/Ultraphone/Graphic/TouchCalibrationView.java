package edu.umich.cse.audioanalysis.Ultraphone.Graphic;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class TouchCalibrationView extends View {

    public TouchCalibrationView(Context context) {
        super(context);
    }

    public TouchCalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchCalibrationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    private void init() {


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        setBackgroundColor(Color.MAGENTA);
        //canvas.drawCircle(ballOrigin.x, ballOrigin.y, ballRadius, paintBall);

    }

}
