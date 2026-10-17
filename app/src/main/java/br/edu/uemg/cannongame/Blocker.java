package br.edu.uemg.cannongame;

/** Obstáculo que penaliza o tempo quando atingido. */
public class Blocker extends GameElement {
    private final int missPenalty;

    public Blocker(CannonView view, int color, int soundId, float x, float y,
                   float width, float length, float velocityY, int missPenalty) {
        super(view, color, soundId, x, y, width, length, velocityY);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }
}
