package by.normalhelth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * A small offline pseudo-3D exercise guide. It draws a shaded figure directly
 * on Canvas, so the instruction works without a video player or internet.
 */
public class MorningExerciseView extends View {
    private static final long STEP_DURATION = 6000L;
    private static final long CYCLE_DURATION = STEP_DURATION * 3L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long startedAt = SystemClock.uptimeMillis();
    private boolean running = true;

    public MorningExerciseView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Анимация утренней разминки");
        setOnClickListener(v -> toggle());
    }

    public void toggle() {
        running = !running;
        if (running) startedAt = SystemClock.uptimeMillis();
        invalidate();
    }

    public boolean isRunning() {
        return running;
    }

    public void startAnimation() {
        running = true;
        startedAt = SystemClock.uptimeMillis();
        invalidate();
    }

    public void stopAnimation() {
        running = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = getWidth();
        float height = getHeight();
        float scale = Math.min(width / (330f * density), height / (360f * density));
        float centerX = width / 2f;
        float baseY = height * 0.83f;

        drawBackground(canvas, width, height, density);
        drawFloor(canvas, centerX, baseY + 7f * density, scale, density);

        long elapsed = running ? (SystemClock.uptimeMillis() - startedAt) % CYCLE_DURATION : 0L;
        int step = (int) (elapsed / STEP_DURATION);
        float stepProgress = (elapsed % STEP_DURATION) / (float) STEP_DURATION;
        float eased = smooth(stepProgress);
        drawPhaseLabel(canvas, width, density, step, eased);
        drawFigure(canvas, centerX, baseY, scale, density, step, eased);

        if (running) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas canvas, float width, float height, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, height,
                Color.rgb(239, 248, 241), Color.rgb(216, 235, 222), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawFloor(Canvas canvas, float centerX, float y, float scale, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(35, 31, 77, 59));
        rect.set(centerX - 105f * scale * density, y - 10f * scale * density,
                centerX + 105f * scale * density, y + 19f * scale * density);
        canvas.drawOval(rect, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f * density);
        paint.setColor(Color.argb(42, 47, 107, 83));
        for (int i = -2; i <= 2; i++) {
            float x = centerX + i * 42f * scale * density;
            canvas.drawLine(x, y - 1f * density, x * 0.94f + centerX * 0.06f,
                    y + 30f * density, paint);
        }
        canvas.drawOval(centerX - 118f * scale * density, y - 15f * scale * density,
                centerX + 118f * scale * density, y + 25f * scale * density, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPhaseLabel(Canvas canvas, float width, float density, int step, float progress) {
        String[] titles = {"Потянуться вверх", "Разбудить плечи", "Шагать на месте"};
        String[] counters = {"1 / 3", "2 / 3", "3 / 3"};
        paint.setShader(null);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18f * density);
        paint.setColor(Color.rgb(31, 77, 59));
        canvas.drawText(titles[step], width / 2f, 31f * density, paint);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        paint.setTextSize(12f * density);
        paint.setColor(Color.rgb(107, 125, 118));
        canvas.drawText(counters[step] + "  ·  мягкий темп", width / 2f, 50f * density, paint);
    }

    private void drawFigure(Canvas canvas, float cx, float baseY, float scale, float density,
                            int step, float progress) {
        float s = scale * density;
        float bodyTop = baseY - 176f * s;
        float shoulderY = bodyTop + 22f * s;
        float hipY = baseY - 72f * s;
        float headY = bodyTop - 26f * s;
        float march = step == 2 ? (float) Math.sin(progress * Math.PI * 2f) : 0f;
        float armLift = step == 0 ? (float) Math.sin(progress * Math.PI) : 0f;
        float shoulderSwing = step == 1 ? (float) Math.sin(progress * Math.PI * 2f) : 0f;

        // Legs first, so the torso sits naturally above them.
        float leftLeg = step == 2 ? march : 0f;
        float rightLeg = step == 2 ? -march : 0f;
        drawLeg(canvas, cx - 13f * s, hipY, -leftLeg, s, density, Color.rgb(64, 87, 116));
        drawLeg(canvas, cx + 13f * s, hipY, -rightLeg, s, density, Color.rgb(64, 87, 116));

        // Arms move opposite to the marching leg and make small shoulder circles.
        drawArm(canvas, cx - 23f * s, shoulderY, false, armLift, shoulderSwing,
                step == 2 ? -rightLeg * 0.45f : 0f, s, density);
        drawArm(canvas, cx + 23f * s, shoulderY, true, armLift, shoulderSwing,
                step == 2 ? -leftLeg * 0.45f : 0f, s, density);

        // Torso with a light-to-dark gradient gives the figure a simple 3D volume.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx - 31f * s, bodyTop, cx + 35f * s, bodyTop,
                Color.rgb(89, 159, 119), Color.rgb(31, 96, 68), Shader.TileMode.CLAMP));
        rect.set(cx - 30f * s, bodyTop, cx + 30f * s, hipY + 8f * s);
        canvas.drawRoundRect(rect, 20f * s, 20f * s, paint);
        paint.setShader(null);
        drawJoint(canvas, cx, hipY + 2f * s, 7f * s, Color.rgb(47, 107, 83));

        // Head as a shaded sphere.
        paint.setShader(new RadialGradient(cx - 6f * s, headY - 6f * s, 24f * s,
                new int[]{Color.rgb(255, 229, 200), Color.rgb(207, 143, 103)},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, headY, 22f * s, paint);
        paint.setShader(null);
        paint.setColor(Color.rgb(81, 54, 45));
        canvas.drawCircle(cx, headY - 16f * s, 15f * s, paint);
        paint.setColor(Color.rgb(255, 240, 220));
        canvas.drawCircle(cx - 7f * s, headY - 7f * s, 2.2f * s, paint);
        canvas.drawCircle(cx + 7f * s, headY - 7f * s, 2.2f * s, paint);
    }

    private void drawLeg(Canvas canvas, float hipX, float hipY, float movement, float s,
                         float density, int color) {
        float kneeX = hipX + movement * 29f * s;
        float kneeY = hipY + 47f * s - Math.abs(movement) * 7f * s;
        float footX = hipX - movement * 14f * s;
        float footY = hipY + 101f * s - Math.abs(movement) * 13f * s;
        drawSegment(canvas, hipX, hipY, kneeX, kneeY, 15f * s, color, density);
        drawSegment(canvas, kneeX, kneeY, footX, footY, 13f * s, color, density);
        drawSegment(canvas, footX - 3f * s, footY, footX + 17f * s, footY, 9f * s,
                Color.rgb(236, 164, 105), density);
        drawJoint(canvas, kneeX, kneeY, 7f * s, color);
    }

    private void drawArm(Canvas canvas, float shoulderX, float shoulderY, boolean right,
                         float lift, float circle, float marchSwing, float s, float density) {
        float side = right ? 1f : -1f;
        float elbowX;
        float elbowY;
        float handX;
        float handY;
        if (lift > 0f) {
            elbowX = shoulderX + side * (36f + 8f * lift) * s;
            elbowY = shoulderY - (22f + 35f * lift) * s;
            handX = shoulderX + side * (42f + 8f * lift) * s;
            handY = shoulderY - (60f + 73f * lift) * s;
        } else if (Math.abs(circle) > 0.01f) {
            elbowX = shoulderX + side * (46f + 10f * circle) * s;
            elbowY = shoulderY + (6f + 18f * circle) * s;
            handX = shoulderX + side * (66f + 13f * circle) * s;
            handY = shoulderY + (19f + 27f * circle) * s;
        } else {
            elbowX = shoulderX + side * 38f * s;
            elbowY = shoulderY + 47f * s + marchSwing * 14f * s;
            handX = shoulderX + side * 31f * s;
            handY = shoulderY + 91f * s + marchSwing * 19f * s;
        }
        drawSegment(canvas, shoulderX, shoulderY, elbowX, elbowY, 12f * s,
                Color.rgb(89, 159, 119), density);
        drawSegment(canvas, elbowX, elbowY, handX, handY, 10f * s,
                Color.rgb(89, 159, 119), density);
        drawJoint(canvas, shoulderX, shoulderY, 7f * s, Color.rgb(47, 107, 83));
        drawJoint(canvas, elbowX, elbowY, 6f * s, Color.rgb(69, 126, 91));
        drawJoint(canvas, handX, handY, 7f * s, Color.rgb(255, 229, 200));
    }

    private void drawSegment(Canvas canvas, float x1, float y1, float x2, float y2,
                             float width, int color, float density) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width + 3f * density);
        paint.setColor(Color.argb(45, 24, 58, 43));
        canvas.drawLine(x1 + 2f * density, y1 + 3f * density,
                x2 + 2f * density, y2 + 3f * density, paint);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawJoint(Canvas canvas, float x, float y, float radius, int color) {
        paint.setShader(new RadialGradient(x - radius * 0.35f, y - radius * 0.4f, radius,
                Color.WHITE, color, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);
    }

    private float smooth(float value) {
        return value * value * (3f - 2f * value);
    }
}
