package by.normalhelth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/**
 * An offline pseudo-3D exercise guide. The movement is drawn as a small human
 * rig rather than as unrelated moving shapes, which keeps the sequence clear.
 */
public class MorningExerciseView extends View {
    private static final long STEP_DURATION = 8000L;
    private static final long CYCLE_DURATION = STEP_DURATION * 3L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long startedAt = SystemClock.uptimeMillis();
    private long pausedElapsed;
    private boolean running = true;

    public MorningExerciseView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Понятная анимация утренней разминки");
        setOnClickListener(v -> toggle());
    }

    public void toggle() {
        if (running) {
            pausedElapsed = elapsed();
            running = false;
        } else {
            startedAt = SystemClock.uptimeMillis() - pausedElapsed;
            running = true;
        }
        invalidate();
    }

    public boolean isRunning() {
        return running;
    }

    public void startAnimation() {
        startedAt = SystemClock.uptimeMillis();
        pausedElapsed = 0L;
        running = true;
        invalidate();
    }

    public void stopAnimation() {
        running = false;
        pausedElapsed = 0L;
        invalidate();
    }

    private long elapsed() {
        return running
                ? (SystemClock.uptimeMillis() - startedAt) % CYCLE_DURATION
                : pausedElapsed;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = getWidth();
        float height = getHeight();
        float widthDp = width / density;
        float heightDp = height / density;
        // Coordinates below are in a small, readable stage measured in dp.
        float s = density * Math.min((widthDp - 34f) / 320f, (heightDp - 64f) / 280f);
        s = Math.max(density * 0.65f, Math.min(s, density * 1.15f));
        float centerX = width / 2f;
        float floorY = height - 30f * s;

        drawBackground(canvas, width, height);
        drawFloor(canvas, centerX, floorY, s, density);

        long time = elapsed();
        int step = (int) (time / STEP_DURATION);
        float stepProgress = (time % STEP_DURATION) / (float) STEP_DURATION;
        drawPhaseLabel(canvas, width, density, step);
        drawFigure(canvas, centerX, floorY, s, density, step, stepProgress);

        if (running) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas canvas, float width, float height) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, height,
                Color.rgb(244, 250, 245), Color.rgb(211, 234, 219), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawFloor(Canvas canvas, float centerX, float floorY, float s, float density) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(42, 25, 67, 48));
        canvas.drawOval(centerX - 104f * s, floorY - 10f * s,
                centerX + 104f * s, floorY + 19f * s, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(Color.argb(42, 47, 107, 83));
        canvas.drawOval(centerX - 118f * s, floorY - 17f * s,
                centerX + 118f * s, floorY + 25f * s, paint);
        for (int i = -2; i <= 2; i++) {
            float x = centerX + i * 43f * s;
            canvas.drawLine(x, floorY - 2f * s, centerX + i * 30f * s,
                    floorY + 25f * s, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPhaseLabel(Canvas canvas, float width, float density, int step) {
        String[] titles = {"Потянуться вверх", "Круги плечами", "Шагать на месте"};
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans", Typeface.BOLD));
        paint.setTextSize(18f * density);
        paint.setColor(Color.rgb(31, 77, 59));
        canvas.drawText(titles[step], width / 2f, 31f * density, paint);
        paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        paint.setTextSize(12f * density);
        paint.setColor(Color.rgb(107, 125, 118));
        canvas.drawText((step + 1) + " / 3  ·  медленно и без рывков", width / 2f,
                50f * density, paint);
    }

    private void drawFigure(Canvas canvas, float cx, float floorY, float s, float density,
                            int step, float rawProgress) {
        float progress = smooth(rawProgress);
        float shoulderY = floorY - 192f * s;
        float hipY = floorY - 101f * s;
        float headY = floorY - 238f * s;
        float bodyTop = floorY - 214f * s;
        float bodyBottom = floorY - 107f * s;

        float leftLegSwing = 0f;
        float rightLegSwing = 0f;
        if (step == 2) {
            // Two complete, even marching cycles. Both feet start and finish on the floor.
            float march = (float) Math.sin(progress * Math.PI * 4f);
            leftLegSwing = march;
            rightLegSwing = -march;
        }

        drawLeg(canvas, cx - 14f * s, hipY, leftLegSwing, s, density, false);
        drawLeg(canvas, cx + 14f * s, hipY, rightLegSwing, s, density, true);

        if (step == 0) {
            float lift = reachAmount(progress);
            drawReachArm(canvas, cx - 29f * s, shoulderY, false, lift, s, density);
            drawReachArm(canvas, cx + 29f * s, shoulderY, true, lift, s, density);
        } else if (step == 1) {
            drawShoulderArm(canvas, cx - 29f * s, shoulderY, false, progress, s, density);
            drawShoulderArm(canvas, cx + 29f * s, shoulderY, true, progress, s, density);
        } else {
            float swing = (float) Math.sin(progress * Math.PI * 4f);
            drawMarchArm(canvas, cx - 29f * s, shoulderY, false, -swing, s, density);
            drawMarchArm(canvas, cx + 29f * s, shoulderY, true, swing, s, density);
        }

        // Torso is drawn after limbs to create clean joints and a simple volumetric shirt.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx - 34f * s, bodyTop, cx + 34f * s, bodyTop,
                Color.rgb(105, 173, 132), Color.rgb(30, 91, 65), Shader.TileMode.CLAMP));
        rect.set(cx - 30f * s, bodyTop, cx + 30f * s, bodyBottom);
        canvas.drawRoundRect(rect, 19f * s, 19f * s, paint);
        paint.setShader(null);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(cx - 22f * s, bodyTop + 9f * s,
                cx - 13f * s, bodyBottom - 14f * s, 5f * s, 5f * s, paint);
        drawJoint(canvas, cx, bodyBottom, 7f * s, Color.rgb(47, 107, 83));

        // Head: a shaded sphere with a small highlight makes orientation readable.
        paint.setShader(new RadialGradient(cx - 7f * s, headY - 8f * s, 25f * s,
                new int[]{Color.rgb(255, 235, 213), Color.rgb(205, 139, 98)},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, headY, 22f * s, paint);
        paint.setShader(null);
        paint.setColor(Color.rgb(79, 54, 45));
        canvas.drawArc(cx - 21f * s, headY - 23f * s, cx + 21f * s, headY + 11f * s,
                180f, 180f, true, paint);
        paint.setColor(Color.rgb(255, 248, 235));
        canvas.drawCircle(cx - 7f * s, headY - 6f * s, 2.2f * s, paint);
        canvas.drawCircle(cx + 7f * s, headY - 6f * s, 2.2f * s, paint);
    }

    private float reachAmount(float progress) {
        // Down -> up -> two-count hold -> down, with no hard pose changes.
        if (progress < 0.28f) return smooth(progress / 0.28f);
        if (progress < 0.55f) return 1f;
        if (progress < 0.72f) return 1f - smooth((progress - 0.55f) / 0.17f) * 0.08f;
        return 0.92f * (1f - smooth((progress - 0.72f) / 0.28f));
    }

    private void drawLeg(Canvas canvas, float hipX, float hipY, float swing, float s,
                         float density, boolean right) {
        float kneeX = hipX + swing * 22f * s;
        float kneeY = hipY + 47f * s - Math.max(0f, swing) * 9f * s;
        float footX = hipX + swing * 39f * s;
        float footY = hipY + 100f * s - Math.max(0f, swing) * 13f * s;
        drawSegment(canvas, hipX, hipY, kneeX, kneeY, 15f * s,
                Color.rgb(65, 87, 116), density);
        drawSegment(canvas, kneeX, kneeY, footX, footY, 13f * s,
                Color.rgb(65, 87, 116), density);
        drawJoint(canvas, kneeX, kneeY, 7f * s, Color.rgb(65, 87, 116));
        float shoeDirection = right ? -1f : 1f;
        drawSegment(canvas, footX, footY, footX + shoeDirection * 18f * s, footY,
                9f * s, Color.rgb(233, 159, 99), density);
    }

    private void drawReachArm(Canvas canvas, float shoulderX, float shoulderY, boolean right,
                              float lift, float s, float density) {
        float side = right ? 1f : -1f;
        float elbowX = shoulderX + side * (39f + 5f * lift) * s;
        float elbowY = shoulderY + (45f - 68f * lift) * s;
        float handX = shoulderX + side * (34f + 3f * lift) * s;
        float handY = shoulderY + (92f - 144f * lift) * s;
        drawArmSegments(canvas, shoulderX, shoulderY, elbowX, elbowY, handX, handY, s, density);
    }

    private void drawShoulderArm(Canvas canvas, float shoulderX, float shoulderY, boolean right,
                                 float progress, float s, float density) {
        float side = right ? 1f : -1f;
        float pose;
        float handX;
        float handY;
        float elbowX;
        float elbowY;
        if (progress < 0.16f) {
            pose = smooth(progress / 0.16f);
            elbowX = shoulderX + side * (39f + 13f * pose) * s;
            elbowY = shoulderY + (45f - 42f * pose) * s;
            handX = shoulderX + side * (34f + 66f * pose) * s;
            handY = shoulderY + (92f - 88f * pose) * s;
        } else if (progress > 0.84f) {
            pose = smooth((progress - 0.84f) / 0.16f);
            elbowX = shoulderX + side * (52f - 13f * pose) * s;
            elbowY = shoulderY + (3f + 42f * pose) * s;
            handX = shoulderX + side * (100f - 66f * pose) * s;
            handY = shoulderY + (4f + 88f * pose) * s;
        } else {
            float circleProgress = (progress - 0.16f) / 0.68f;
            float angle = (float) (circleProgress * Math.PI * 4f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            elbowX = shoulderX + side * (51f + 7f * cos) * s;
            elbowY = shoulderY + (3f + 12f * sin) * s;
            handX = shoulderX + side * (92f + 13f * cos) * s;
            handY = shoulderY + (4f + 27f * sin) * s;
        }
        drawArmSegments(canvas, shoulderX, shoulderY, elbowX, elbowY, handX, handY, s, density);
    }

    private void drawMarchArm(Canvas canvas, float shoulderX, float shoulderY, boolean right,
                              float swing, float s, float density) {
        float side = right ? 1f : -1f;
        float elbowX = shoulderX + side * (38f + swing * 11f) * s;
        float elbowY = shoulderY + (44f + Math.abs(swing) * 4f) * s;
        float handX = shoulderX + side * (31f + swing * 19f) * s;
        float handY = shoulderY + (88f + Math.abs(swing) * 5f) * s;
        drawArmSegments(canvas, shoulderX, shoulderY, elbowX, elbowY, handX, handY, s, density);
    }

    private void drawArmSegments(Canvas canvas, float shoulderX, float shoulderY,
                                 float elbowX, float elbowY, float handX, float handY,
                                 float s, float density) {
        drawSegment(canvas, shoulderX, shoulderY, elbowX, elbowY, 12f * s,
                Color.rgb(91, 158, 116), density);
        drawSegment(canvas, elbowX, elbowY, handX, handY, 10f * s,
                Color.rgb(91, 158, 116), density);
        drawJoint(canvas, shoulderX, shoulderY, 7f * s, Color.rgb(47, 107, 83));
        drawJoint(canvas, elbowX, elbowY, 6f * s, Color.rgb(70, 128, 94));
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
        // Narrow highlight on each limb makes the direction and bend easier to follow.
        paint.setStrokeWidth(Math.max(1f, width * 0.18f));
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawLine(x1 - density, y1 - density, x2 - density, y2 - density, paint);
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
