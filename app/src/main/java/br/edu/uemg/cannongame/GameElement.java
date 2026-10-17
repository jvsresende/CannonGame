package br.edu.uemg.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Elemento retangular do jogo (base para Blocker, Target e Cannonball). */
public class GameElement {
    protected final CannonView view;
    protected final Paint paint = new Paint();
    protected final RectF shape;
    protected float velocityY;
    private final int soundId;
    private final int color;

    public GameElement(CannonView view, int color, int soundId, float x, float y,
                       float width, float length, float velocityY) {
        this.view = view;
        this.color = color;
        this.soundId = soundId;
        this.velocityY = velocityY;
        paint.setColor(color);
        paint.setAntiAlias(true);
        shape = new RectF(x, y, x + width, y + length);
    }

    /** Atualiza a posição vertical; inverte a direção ao tocar o topo/fundo da tela. */
    public void update(double interval) {
        shape.offset(0, (float) (velocityY * interval));

        if ((shape.top < 0 && velocityY < 0)
                || (shape.bottom > view.getScreenHeight() && velocityY > 0)) {
            velocityY *= -1;
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    public void playSound() {
        view.playSound(soundId);
    }

    public int getColor() {
        return color;
    }
}
