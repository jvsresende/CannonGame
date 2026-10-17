package br.edu.uemg.cannongame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Menu principal: Jogar, Minha pontuação, Ranking e Sair. */
public class MenuActivity extends AppCompatActivity {

    private HighScoreManager highScores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        highScores = new HighScoreManager(this);

        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, MainActivity.class));
            }
        });

        findViewById(R.id.btnMyScore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMyScore();
            }
        });

        findViewById(R.id.btnRanking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRanking();
            }
        });

        findViewById(R.id.btnExit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });
    }

    private void showMyScore() {
        String message = getString(R.string.my_score_format,
                highScores.getBestScore(),
                highScores.getLastScore(),
                highScores.getGamesPlayed(),
                highScores.getBestLevel());
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_my_score)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void showRanking() {
        List<Integer> scores = highScores.getScores();
        String message;
        if (scores.isEmpty()) {
            message = getString(R.string.ranking_empty);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < scores.size(); i++) {
                if (i > 0) sb.append('\n');
                sb.append(i + 1).append("º  -  ").append(scores.get(i)).append(" pontos");
            }
            message = sb.toString();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ranking_title)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    // Modo imersivo também no menu
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
