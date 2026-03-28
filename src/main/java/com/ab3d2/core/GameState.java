package com.ab3d2.core;

/**
 * États possibles du jeu.
 * On utilise un sealed interface + records pour un pattern matching propre (Java 21).
 */
public sealed interface GameState permits
        GameState.MainMenu,
        GameState.LevelSelect,
        GameState.InGame,
        GameState.LevelEditor,
        GameState.Options,
        GameState.Loading,
        GameState.Credits {

    record MainMenu() implements GameState {}
    record LevelSelect() implements GameState {}
    record InGame(int levelIndex) implements GameState {}
    record LevelEditor(String levelPath) implements GameState {}
    record Options() implements GameState {}
    record Loading(GameState next, String message) implements GameState {}
    record Credits() implements GameState {}
}
