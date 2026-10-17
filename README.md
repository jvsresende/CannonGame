# Cannon Game

Aplicação Android do **Cannon Game** (Capítulo 6 do livro *Android 6 for Programmers: An App-Driven Approach*, 3ª edição), desenvolvida para a disciplina de **Programação III** (UEMG – Unidade Passos).

## Integrantes

- Arthur Garlati
- Carol Pádua
- João Victor Rezende

## Objetivo

Aplicar, em um jogo completo, conceitos de programação mobile em Android: animação quadro a quadro, Canvas e Paint, sons, threads e game loop, `SurfaceView`/`SurfaceHolder`, modo imersivo e interação por toque.

## Menu principal

Jogar, Minha pontuação (melhor, última, partidas jogadas e maior nível), Ranking (Top 5) e Sair.

## Como jogar

- **Toque simples:** mira o canhão no ponto tocado e atira.
- **Arrastar para cima/baixo:** aumenta/diminui o ângulo do canhão.
- **Arrastar para a direita/esquerda:** aumenta/diminui a força do tiro.
- Acertar um alvo dá **+3 s**; acertar o bloqueador tira **2 s** e pontos.
- Destrua os 9 alvos antes do tempo acabar para avançar de nível.

## Funcionalidades implementadas

**Requisitos do Capítulo 6**
- Game loop em `Thread` dedicada com `SurfaceView` e `SurfaceHolder`
- Desenho com `Canvas` e `Paint`; animação quadro a quadro
- Sons com `SoundPool` (disparo, acerto no alvo, acerto no bloqueador)
- Detecção de colisão, 9 alvos móveis e bloqueador
- Modo imersivo (tela cheia) e diálogo de fim de jogo

**Recursos extras**
- **Pontuação com multiplicadores:** sequência de acertos aumenta o multiplicador (até x5); errar zera a sequência.
- **Dificuldade progressiva:** a cada acerto os alvos restantes ficam mais rápidos e menores; a cada nível, a velocidade aumenta e o tamanho diminui; a direção dos alvos alterna.
- **Controle por toque direcional (swipe):** ângulo e força ajustados por gestos.
- **Partículas ao acertar o alvo:** explosão radial de círculos coloridos com fade out.
- **Ranking local (Top 5):** persistido com `SharedPreferences`.

## Estrutura

```
app/src/main/java/br/edu/uemg/cannongame/
  MenuActivity.java        Menu principal
  MainActivity.java        Tela do jogo e modo imersivo
  CannonGameFragment.java  Fragmento e ciclo de vida
  CannonView.java          SurfaceView, game loop, regras, toque
  Cannon / Cannonball      Canhão e bala
  GameElement / Target / Blocker
  Particle.java            Partículas
  HighScoreManager.java    Ranking (SharedPreferences)
app/src/main/res/raw/      Sons (.wav)
```

## Screenshots

