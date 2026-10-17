package br.edu.uemg.cannongame;

/** Alvo que concede tempo extra quando atingido. Suporta dificuldade progressiva. */
public class Target extends GameElement {
    public enum MotionType { VERTICAL, SINE, ZIGZAG }

    private final int hitReward;
    private final float minLength;
    private final MotionType motionType;
    private final float baseX;
    private final float baseCenterY;
    private final float horizontalRange;
    private final float phaseOffset;
    private float horizontalSpeed;
    private double elapsedTime;

    public Target(CannonView view, int color, int soundId, float x, float y,
                  float width, float length, float velocityY, int hitReward,
                  MotionType motionType, float horizontalRange, float horizontalSpeed,
                  float phaseOffset) {
        super(view, color, soundId, x, y, width, length, velocityY);
        this.hitReward = hitReward;
        this.minLength = length * 0.4f; // o alvo nunca fica menor que 40% do tamanho inicial
        this.motionType = motionType;
        this.baseX = x;
        this.baseCenterY = shape.centerY();
        this.horizontalRange = horizontalRange;
        this.horizontalSpeed = horizontalSpeed;
        this.phaseOffset = phaseOffset;
    }

    public int getHitReward() {
        return hitReward;
    }

    /** Aumenta a velocidade (mantendo a direção atual). */
    public void speedUp(float factor) {
        velocityY *= factor;
        if (motionType == MotionType.ZIGZAG) {
            horizontalSpeed *= factor;
        }
    }

    /** Diminui a altura do alvo mantendo o centro. */
    public void shrink(float factor) {
        float newLength = Math.max(minLength, shape.height() * factor);
        float centerY = shape.centerY();
        shape.top = centerY - newLength / 2f;
        shape.bottom = centerY + newLength / 2f;
    }

    @Override
    public void update(double interval) {
        if (motionType == MotionType.SINE) {
            elapsedTime += interval;
            float amplitude = view.getScreenHeight() * 0.20f;
            float frequency = Math.abs(velocityY)
                    / Math.max(view.getScreenHeight() * 0.35f, 1f);
            float centerY = baseCenterY
                    + (float) Math.sin(elapsedTime * frequency + phaseOffset) * amplitude;
            shape.offsetTo(shape.left, centerY - shape.height() / 2f);
            return;
        }

        super.update(interval);

        if (motionType == MotionType.ZIGZAG) {
            float minX = Math.max(0, baseX - horizontalRange);
            float maxX = Math.min(view.getScreenWidth() - shape.width(),
                    baseX + horizontalRange);
            float nextX = shape.left + horizontalSpeed * (float) interval;

            if (nextX < minX) {
                nextX = minX;
                horizontalSpeed = Math.abs(horizontalSpeed);
            } else if (nextX > maxX) {
                nextX = maxX;
                horizontalSpeed = -Math.abs(horizontalSpeed);
            }

            shape.offsetTo(nextX, shape.top);
        }
    }
}
