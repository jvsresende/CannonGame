package br.edu.uemg.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Partícula circular com movimento radial e fade out. */
public class Particle {
    private float x;
    private float y;
    private final float vx;
    private final float vy;
    private final float radius;
    private final int color;
    private final float maxLife;
    private float life;

    public Particle(float x, float y, float vx, float vy, float radius, int color, float life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
        this.maxLife = life;
        this.life = life;
    }

    /** @return true enquanto a partícula ainda está viva. */
    public boolean update(float dt) {
        x += vx * dt;
        y += vy * dt;
        life -= dt;
        return life > 0;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        paint.setAlpha(Math.max(0, (int) (255 * life / maxLife)));
        canvas.drawCircle(x, y, radius, paint);
    }
}
