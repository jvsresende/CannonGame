package br.edu.uemg.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

/** Canhão: mira (ângulo), força de disparo e a bala em voo. */
public class Cannon {
    private static final double MAX_ANGLE = Math.toRadians(70);
    private static final float MIN_POWER = 0.5f;
    private static final float MAX_POWER = 1.5f;

    private final CannonView view;
    private final int baseRadius;
    private final int barrelLength;
    private final PointF barrelEnd = new PointF();
    private final Paint paint = new Paint();

    private double angle = 0;   // radianos; positivo = cano apontando para cima
    private float power = 1f;   // multiplicador da velocidade da bala
    private Cannonball cannonball;

    public Cannon(CannonView view, int baseRadius, int barrelLength, int barrelWidth) {
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;
        paint.setStrokeWidth(barrelWidth);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        align(0);
    }

    private float baseY() {
        return view.getScreenHeight() / 2f;
    }

    /** Define o ângulo do cano (radianos), limitado a +-70 graus. */
    public void align(double newAngle) {
        angle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, newAngle));
        barrelEnd.set((float) (barrelLength * Math.cos(angle)),
                (float) (baseY() - barrelLength * Math.sin(angle)));
    }

    /** Aponta o cano para o ponto tocado. */
    public void alignToTouch(float x, float y) {
        align(Math.atan2(baseY() - y, Math.max(x, 1f)));
    }

    public void adjustAngle(double delta) {
        align(angle + delta);
    }

    public void adjustPower(float delta) {
        power = Math.max(MIN_POWER, Math.min(MAX_POWER, power + delta));
    }

    public int getAngleDegrees() {
        return (int) Math.round(Math.toDegrees(angle));
    }

    public int getPowerPercent() {
        return Math.round(power * 100);
    }

    public void fireCannonball() {
        float radius = view.getScreenHeight() * CannonView.CANNONBALL_RADIUS_PERCENT;
        float speed = view.getScreenWidth() * CannonView.CANNONBALL_SPEED_PERCENT * power;

        cannonball = new Cannonball(view, Color.BLACK, CannonView.CANNON_SOUND_ID,
                barrelEnd.x, barrelEnd.y, radius,
                (float) (speed * Math.cos(angle)), (float) (-speed * Math.sin(angle)));
        cannonball.playSound();
    }

    public Cannonball getCannonball() {
        return cannonball;
    }

    public void removeCannonball() {
        cannonball = null;
    }

    public void draw(Canvas canvas) {
        canvas.drawLine(0, baseY(), barrelEnd.x, barrelEnd.y, paint);
        canvas.drawCircle(0, baseY(), baseRadius, paint);
    }
}
