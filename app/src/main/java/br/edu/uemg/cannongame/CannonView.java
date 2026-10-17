package br.edu.uemg.cannongame;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * View principal do jogo (SurfaceView). Contém o game loop (thread), o desenho com
 * Canvas/Paint, os sons (SoundPool), o controle por toque e as regras do jogo.
 */
public class CannonView extends SurfaceView implements SurfaceHolder.Callback {

    // ---- Constantes do Capítulo 6 ----
    public static final int MISS_PENALTY = 2;   // segundos perdidos ao acertar o bloqueador
    public static final int HIT_REWARD = 3;     // segundos ganhos ao acertar um alvo

    public static final float CANNON_BASE_RADIUS_PERCENT = 3f / 40;
    public static final float CANNON_BARREL_WIDTH_PERCENT = 3f / 40;
    public static final float CANNON_BARREL_LENGTH_PERCENT = 1f / 10;
    public static final float CANNONBALL_RADIUS_PERCENT = 3f / 80;
    public static final float CANNONBALL_SPEED_PERCENT = 3f / 2;

    public static final float TARGET_WIDTH_PERCENT = 1f / 40;
    public static final float TARGET_LENGTH_PERCENT = 3f / 20;
    public static final float TARGET_FIRST_X_PERCENT = 3f / 5;
    public static final float TARGET_SPACING_PERCENT = 1f / 60;
    public static final int TARGET_PIECES = 9;
    public static final float TARGET_MIN_SPEED_PERCENT = 3f / 4;
    public static final float TARGET_MAX_SPEED_PERCENT = 6f / 4;

    public static final float BLOCKER_WIDTH_PERCENT = 1f / 40;
    public static final float BLOCKER_LENGTH_PERCENT = 1f / 4;
    public static final float BLOCKER_X_PERCENT = 1f / 2;
    public static final float BLOCKER_SPEED_PERCENT = 1f;

    public static final float TEXT_SIZE_PERCENT = 1f / 18;

    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    public static final int FAIL_SOUND_ID = 3;

    // ---- Extras: pontuação, dificuldade progressiva, partículas ----
    private static final int POINTS_PER_HIT = 100;
    private static final int BLOCKER_SCORE_PENALTY = 50;
    private static final int MAX_MULTIPLIER = 5;

    private static final float HIT_SPEEDUP = 1.08f;     // alvos restantes ficam 8% mais rápidos a cada acerto
    private static final float HIT_SHRINK = 0.95f;      // ... e 5% menores
    private static final float LEVEL_SPEEDUP = 0.15f;   // +15% de velocidade por nível
    private static final float LEVEL_SHRINK = 0.10f;    // -10% de tamanho por nível
    private static final float MIN_LEVEL_SIZE = 0.5f;

    private static final int PARTICLE_COUNT = 24;
    private static final double INITIAL_TIME = 10.0;

    private CannonThread cannonThread;
    private volatile boolean surfaceReady;
    private final Activity activity;
    private boolean dialogIsDisplayed = false;

    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    private int screenWidth;
    private int screenHeight;
    private volatile boolean gameOver;
    private double timeLeft;
    private double totalElapsedTime;
    private int shotsFired;

    private int score;
    private int roundStartScore;
    private int streak;
    private int maxStreak;
    private int level = 1;
    private boolean resultSaved;

    private final Random random = new Random();
    private final SoundPool soundPool;
    private final SparseIntArray soundMap;
    private final MediaPlayer backgroundMusic;
    private final HighScoreManager highScores;

    private final Paint textPaint = new Paint();
    private final Paint hudPaint = new Paint();
    private final Paint backgroundImagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect backgroundRect = new Rect();
    private final Bitmap backgroundBitmap;
    private final Paint particlePaint = new Paint();

    // Controle por toque direcional (swipe)
    private final int touchSlop;
    private float downX, downY, lastX, lastY;
    private boolean dragging;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = findActivity(context);
        getHolder().addCallback(this);
        highScores = new HighScoreManager(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build();

        soundMap = new SparseIntArray(4);
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));
        soundMap.put(FAIL_SOUND_ID, soundPool.load(context, R.raw.fail, 1));
        backgroundMusic = MediaPlayer.create(context, R.raw.background_music);
        if (backgroundMusic != null) {
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.15f, 0.15f);
        }
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.game_background);

        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.rgb(23, 74, 126));
        hudPaint.setAntiAlias(true);
        hudPaint.setColor(Color.rgb(168, 70, 0));
        particlePaint.setAntiAlias(true);
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        throw new IllegalStateException("CannonView precisa ser usada dentro de uma Activity");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        textPaint.setTextSize(TEXT_SIZE_PERCENT * h);
        hudPaint.setTextSize(TEXT_SIZE_PERCENT * h * 0.6f);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    // ---------------------------------------------------------------------
    // Novo jogo / nova rodada
    // ---------------------------------------------------------------------
    public void newGame() {
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        // Dificuldade progressiva por nível
        float levelSpeed = 1f + LEVEL_SPEEDUP * (level - 1);
        float levelSize = Math.max(MIN_LEVEL_SIZE, 1f - LEVEL_SHRINK * (level - 1));

        blocker = new Blocker(this, Color.rgb(60, 60, 60), BLOCKER_SOUND_ID,
                BLOCKER_X_PERCENT * screenWidth,
                (0.5f - BLOCKER_LENGTH_PERCENT / 2) * screenHeight,
                BLOCKER_WIDTH_PERCENT * screenWidth,
                BLOCKER_LENGTH_PERCENT * screenHeight,
                BLOCKER_SPEED_PERCENT * screenHeight * levelSpeed,
                MISS_PENALTY);

        targets = new ArrayList<>();
        float targetWidth = TARGET_WIDTH_PERCENT * screenWidth;
        float targetLength = TARGET_LENGTH_PERCENT * screenHeight * levelSize;
        float targetX = TARGET_FIRST_X_PERCENT * screenWidth;
        float targetSpacing = TARGET_SPACING_PERCENT * screenWidth;

        for (int i = 0; i < TARGET_PIECES; i++) {
            float speed = screenHeight * (random.nextFloat()
                    * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT)
                    + TARGET_MIN_SPEED_PERCENT) * levelSpeed;
            if (i % 2 == 0) {
                speed = -speed; // direção alternada entre os alvos
            }
            int color = (i % 2 == 0)
                    ? Color.rgb(0, 158, 115) : Color.rgb(213, 94, 0);

            Target.MotionType motionType;
            if (i % 3 == 1) {
                motionType = Target.MotionType.SINE;
            } else if (i % 3 == 2) {
                motionType = Target.MotionType.ZIGZAG;
            } else {
                motionType = Target.MotionType.VERTICAL;
            }
            float horizontalSpeed = (i % 2 == 0 ? 1 : -1)
                    * screenWidth * 0.035f * levelSpeed;

            targets.add(new Target(this, color, TARGET_SOUND_ID, targetX,
                    (screenHeight - targetLength) / 2f, targetWidth, targetLength,
                    speed, HIT_REWARD, motionType, targetSpacing * 0.40f,
                    motionType == Target.MotionType.ZIGZAG ? horizontalSpeed : 0,
                    i * 0.8f));
            targetX += targetWidth + targetSpacing;
        }

        particles.clear();
        timeLeft = INITIAL_TIME;
        if (level == 1 && roundStartScore == 0) {
            shotsFired = 0;
            totalElapsedTime = 0.0;
            maxStreak = 0;
            resultSaved = false;
        }
        score = roundStartScore;
        streak = 0;
        gameOver = false;
    }

    private void startGameThread() {
        if (!surfaceReady || (cannonThread != null && cannonThread.isAlive())) return;
        startBackgroundMusic();
        cannonThread = new CannonThread(getHolder());
        cannonThread.setRunning(true);
        cannonThread.start();
    }

    public void resumeGame() {
        if (surfaceReady && !gameOver && cannon != null) startGameThread();
    }

    private void startBackgroundMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    private void pauseBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    // ---------------------------------------------------------------------
    // Lógica do jogo
    // ---------------------------------------------------------------------
    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0;

        Cannonball ball = cannon.getCannonball();
        if (ball != null) {
            ball.update(interval);
        }
        blocker.update(interval);
        for (GameElement target : targets) {
            target.update(interval);
        }
        updateParticles((float) interval);

        ball = cannon.getCannonball();
        if (ball != null) {
            if (!ball.isOnScreen()) {
                // errou o tiro: perde a sequência
                cannon.removeCannonball();
                streak = 0;
                playSound(FAIL_SOUND_ID);
            } else if (ball.collidesWith(blocker)) {
                blocker.playSound();
                ball.reverseVelocityX();
                timeLeft -= blocker.getMissPenalty();
                score = Math.max(0, score - BLOCKER_SCORE_PENALTY);
                streak = 0;
            } else {
                for (int n = 0; n < targets.size(); n++) {
                    Target target = targets.get(n);
                    if (ball.collidesWith(target)) {
                        onTargetHit(target);
                        break;
                    }
                }
            }
        }

        timeLeft -= interval;

        if (timeLeft <= 0) {
            timeLeft = 0;
            endGame(false);
        } else if (targets.isEmpty()) {
            endGame(true);
        }
    }

    private void onTargetHit(Target target) {
        target.playSound();
        timeLeft += target.getHitReward();

        // Pontuação com multiplicador progressivo (sequência de acertos)
        int multiplier = Math.min(1 + streak, MAX_MULTIPLIER);
        score += POINTS_PER_HIT * multiplier;
        streak++;
        maxStreak = Math.max(maxStreak, streak);

        spawnParticles(target.shape.centerX(), target.shape.centerY());

        cannon.removeCannonball();
        targets.remove(target);

        // Dificuldade progressiva: os alvos restantes aceleram e diminuem
        for (Target other : targets) {
            other.speedUp(HIT_SPEEDUP);
            other.shrink(HIT_SHRINK);
        }
    }

    private void spawnParticles(float cx, float cy) {
        int[] palette = {
                Color.rgb(0, 158, 115), Color.rgb(213, 94, 0),
                Color.rgb(23, 74, 126), Color.rgb(235, 150, 85)
        };
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double a = 2 * Math.PI * i / PARTICLE_COUNT + random.nextDouble() * 0.3;
            float speed = screenWidth * (0.12f + random.nextFloat() * 0.25f);
            float radius = screenHeight / 110f * (0.6f + random.nextFloat());
            particles.add(new Particle(cx, cy,
                    (float) (speed * Math.cos(a)), (float) (speed * Math.sin(a)),
                    radius, palette[random.nextInt(palette.length)],
                    0.5f + random.nextFloat() * 0.4f));
        }
    }

    private void updateParticles(float dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            if (!it.next().update(dt)) {
                it.remove();
            }
        }
    }

    private int currentMultiplier() {
        return Math.min(1 + streak, MAX_MULTIPLIER);
    }

    // ---------------------------------------------------------------------
    // Desenho
    // ---------------------------------------------------------------------
    public void drawGameElements(Canvas canvas) {
        if (backgroundBitmap != null) {
            backgroundRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(backgroundBitmap, null, backgroundRect, backgroundImagePaint);
        } else {
            canvas.drawColor(Color.rgb(242, 246, 252));
        }

        float ts = textPaint.getTextSize();
        canvas.drawText(getResources().getString(R.string.time_format, timeLeft, level),
                30, ts * 1.1f, textPaint);
        canvas.drawText(getResources().getString(R.string.score_format, score, currentMultiplier()),
                30, ts * 2.2f, textPaint);
        canvas.drawText(getResources().getString(R.string.aim_format,
                        cannon.getAngleDegrees(), cannon.getPowerPercent()),
                30, ts * 3.0f, hudPaint);

        cannon.draw(canvas);

        Cannonball ball = cannon.getCannonball();
        if (ball != null) {
            ball.draw(canvas);
        }

        blocker.draw(canvas);
        for (GameElement target : targets) {
            target.draw(canvas);
        }
        for (Particle p : particles) {
            p.draw(canvas, particlePaint);
        }
    }

    // ---------------------------------------------------------------------
    // Fim de jogo
    // ---------------------------------------------------------------------
    private void endGame(boolean won) {
        gameOver = true;
        pauseBackgroundMusic();
        if (cannonThread != null) {
            cannonThread.setRunning(false);
        }
        showGameOverDialog(won);
    }

    private void showGameOverDialog(final boolean won) {
        final StringBuilder message = new StringBuilder(getResources().getString(
                R.string.results_format, shotsFired, totalElapsedTime, score, maxStreak, level));

        if (!won) {
            int previousBest = highScores.getBestScore();
            List<Integer> ranking = saveResult();

            if (score > 0 && score > previousBest) {
                message.append("\n\n").append(getResources().getString(R.string.new_record));
            }
            message.append("\n\n").append(getResources().getString(R.string.ranking_title));
            for (int i = 0; i < ranking.size(); i++) {
                message.append("\n").append(i + 1).append(". ").append(ranking.get(i));
            }
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(won ? R.string.win : R.string.lose);
                builder.setMessage(message.toString());
                builder.setCancelable(false);
                builder.setPositiveButton(won ? R.string.next_level : R.string.reset_game,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                dialogIsDisplayed = false;
                                if (won) {
                                    level++;
                                    roundStartScore = score;
                                } else {
                                    level = 1;
                                    roundStartScore = 0;
                                }
                                newGame();
                                startGameThread();
                            }
                        });
                builder.setNegativeButton(R.string.menu,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                dialogIsDisplayed = false;
                                if (!won) {
                                    level = 1;
                                    roundStartScore = 0;
                                } else {
                                    saveResult();
                                }
                                activity.finish(); // volta ao menu
                            }
                        });
                dialogIsDisplayed = true;
                builder.show();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Ciclo de vida
    // ---------------------------------------------------------------------
    public void stopGame() {
        pauseBackgroundMusic();
        CannonThread thread = cannonThread;
        if (thread == null) return;
        thread.setRunning(false);
        if (Thread.currentThread() == thread) return;
        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (cannonThread == thread) cannonThread = null;
        if (interrupted) Thread.currentThread().interrupt();
    }

    public void releaseResources() {
        stopGame();
        if (backgroundMusic != null) backgroundMusic.release();
        soundPool.release();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (!dialogIsDisplayed) {
            newGame();
            startGameThread();
        }
    }

    private List<Integer> saveResult() {
        if (resultSaved) {
            return highScores.getScores();
        }

        highScores.recordGame(score, level);
        List<Integer> ranking = highScores.addScore(score);
        resultSaved = true;
        return ranking;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopGame();
    }

    // ---------------------------------------------------------------------
    // Toque: toque simples = mirar e atirar; arrastar = swipe direcional
    //   arrastar para cima/baixo -> aumenta/diminui o ângulo
    //   arrastar para direita/esquerda -> aumenta/diminui a força
    // ---------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (gameOver || cannon == null) {
            return true;
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                dragging = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                float x = e.getX();
                float y = e.getY();
                if (!dragging && Math.hypot(x - downX, y - downY) > touchSlop) {
                    dragging = true;
                }
                if (dragging) {
                    synchronized (getHolder()) {
                        cannon.adjustAngle(-(y - lastY) * Math.PI / screenHeight);
                        cannon.adjustPower(1.5f * (x - lastX) / screenWidth);
                    }
                }
                lastX = x;
                lastY = y;
                break;
            }

            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    synchronized (getHolder()) {
                        cannon.alignToTouch(e.getX(), e.getY());
                        fireCannonball();
                    }
                }
                performClick();
                break;

            default:
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void fireCannonball() {
        if (cannon.getCannonball() != null) {
            return; // só uma bala por vez
        }
        cannon.fireCannonball();
        ++shotsFired;
    }

    // ---------------------------------------------------------------------
    // Game loop
    // ---------------------------------------------------------------------
    private class CannonThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private volatile boolean threadIsRunning = true;

        CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        void setRunning(boolean running) {
            threadIsRunning = running;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas != null) {
                        synchronized (surfaceHolder) {
                            long currentTime = System.currentTimeMillis();
                            double elapsedTimeMS = Math.min(currentTime - previousFrameTime, 100);
                            totalElapsedTime += elapsedTimeMS / 1000.0;
                            updatePositions(elapsedTimeMS);
                            drawGameElements(canvas);
                            previousFrameTime = currentTime;
                        }
                    }
                } catch (RuntimeException e) {
                    android.util.Log.e("CannonView", "Falha no loop de desenho do jogo", e);
                    threadIsRunning = false;
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (RuntimeException e) {
                            android.util.Log.e("CannonView", "Falha ao liberar o Canvas", e);
                            threadIsRunning = false;
                        }
                        canvas = null;
                    }
                }
            }
        }
    }
}
