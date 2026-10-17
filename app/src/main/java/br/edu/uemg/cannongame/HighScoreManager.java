package br.edu.uemg.cannongame;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ranking local (Top 5) e estatísticas do jogador, persistidos com SharedPreferences. */
public class HighScoreManager {
    private static final String PREFS_NAME = "cannon_game_prefs";
    private static final String KEY_SCORES = "highscores";
    private static final String KEY_LAST_SCORE = "last_score";
    private static final String KEY_GAMES_PLAYED = "games_played";
    private static final String KEY_BEST_LEVEL = "best_level";
    private static final int MAX_SCORES = 5;

    private final SharedPreferences prefs;

    public HighScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Integer> getScores() {
        List<Integer> scores = new ArrayList<>();
        String raw = prefs.getString(KEY_SCORES, "");
        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(",")) {
                try {
                    scores.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                    // ignora valores inválidos
                }
            }
        }
        return scores;
    }

    /** Adiciona a pontuação, mantém apenas o Top 5 e devolve a lista atualizada. */
    public List<Integer> addScore(int score) {
        List<Integer> scores = getScores();
        if (score > 0) {
            scores.add(score);
        }
        Collections.sort(scores, Collections.reverseOrder());
        while (scores.size() > MAX_SCORES) {
            scores.remove(scores.size() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(scores.get(i));
        }
        prefs.edit().putString(KEY_SCORES, sb.toString()).apply();
        return scores;
    }

    /** Registra o fim de uma partida (última pontuação, nº de partidas e maior nível). */
    public void recordGame(int score, int level) {
        prefs.edit()
                .putInt(KEY_LAST_SCORE, score)
                .putInt(KEY_GAMES_PLAYED, getGamesPlayed() + 1)
                .putInt(KEY_BEST_LEVEL, Math.max(getBestLevel(), level))
                .apply();
    }

    public int getBestScore() {
        List<Integer> scores = getScores();
        return scores.isEmpty() ? 0 : scores.get(0);
    }

    public int getLastScore() {
        return prefs.getInt(KEY_LAST_SCORE, 0);
    }

    public int getGamesPlayed() {
        return prefs.getInt(KEY_GAMES_PLAYED, 0);
    }

    public int getBestLevel() {
        return prefs.getInt(KEY_BEST_LEVEL, 0);
    }
}
