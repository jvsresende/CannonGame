package br.edu.uemg.cannongame;

import android.graphics.Canvas;
import android.graphics.RectF;

/** Bala de canhão: move-se em X e Y e é desenhada como círculo. */
public class Cannonball extends GameElement {
    private float velocityX;

    public Cannonball(CannonView view, int color, int soundId, float centerX, float centerY,
                      float radius, float velocityX, float velocityY) {
        super(view, color, soundId, centerX - radius, centerY - radius,
                2 * radius, 2 * radius, velocityY);
        this.velocityX = velocityX;
    }

    private float getRadius() {
        return shape.width() / 2f;
    }

    /** Colisão com outro elemento (só conta enquanto a bala vai para a direita). */
    public boolean collidesWith(GameElement element) {
        return RectF.intersects(shape, element.shape) && velocityX > 0;
    }

    public boolean isOnScreen() {
        return shape.right >= 0 && shape.left <= view.getScreenWidth()
                && shape.bottom >= 0 && shape.top <= view.getScreenHeight();
    }

    public void reverseVelocityX() {
        velocityX *= -1;
    }

    @Override
    public void update(double interval) {
        shape.offset((float) (velocityX * interval), (float) (velocityY * interval));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.centerX(), shape.centerY(), getRadius(), paint);
    }
}
