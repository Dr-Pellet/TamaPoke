package com.tamapoke.app.ui

/** The app has no deep back-stack (mirrors the original's flat swipe-panel model): just a handful of top-level destinations. */
enum class Screen(val emoji: String, val label: String) {
    MAIN("🏠", "Home"),
    POKEDEX("📖", "Pokedex"),
    STATS("📊", "Stats"),
    SETTINGS("⚙️", "Settings"),
}
