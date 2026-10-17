package br.edu.uemg.cannongame;

/** Alvo que concede tempo extra quando atingido. Suporta dificuldade progressiva. */
public class Target extends GameElement {
    private final int hitReward;
    private final float minLength;

    public Target(CannonView view, int color, int soundId, float x, float y,
                  float width, float length, float velocityY, int hitReward) {
        super(view, color, soundId, x, y, width, length, velocityY);
        this.hitReward = hitReward;
        this.minLength = length * 0.4f; // o alvo nunca fica menor que 40% do tamanho inicial
    }

    public int getHitReward() {
        return hitReward;
    }

    /** Aumenta a velocidade (mantendo a direção atual). */
    public void speedUp(float factor) {
        velocityY *= factor;
    }

    /** Diminui a altura do alvo mantendo o centro. */
    public void shrink(float factor) {
        float newLength = Math.max(minLength, shape.height() * factor);
        float centerY = shape.centerY();
        shape.top = centerY - newLength / 2f;
        shape.bottom = centerY + newLength / 2f;
    }
}
