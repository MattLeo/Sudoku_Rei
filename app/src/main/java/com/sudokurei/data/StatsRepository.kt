package com.sudokurei.data

import android.content.Context
import com.sudokurei.game.Difficulty
import com.sudokurei.game.DifficultyStats
import com.sudokurei.game.PlayerStats
import org.json.JSONObject

class StatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sudoku_stats", Context.MODE_PRIVATE)
    private val key = "player_stats"

    fun save(stats: PlayerStats) {
        prefs.edit().putString(key, serialize(stats)).apply()
    }

    fun load(): PlayerStats {
        val json = prefs.getString(key, null) ?: return PlayerStats()
        return try { deserialize(json) } catch (_: Exception) { PlayerStats() }
    }

    fun reset() {
        prefs.edit().remove(key).apply()
    }

    private fun serialize(stats: PlayerStats): String {
        val obj = JSONObject()
        Difficulty.entries.forEach { diff ->
            val ds = stats.forDifficulty(diff)
            obj.put(diff.name, JSONObject().apply {
                put("won",   ds.gamesWon)
                put("best",  ds.bestTime)
                put("total", ds.totalTime)
            })
        }
        return obj.toString()
    }

    private fun deserialize(json: String): PlayerStats {
        val obj = JSONObject(json)
        var stats = PlayerStats()
        Difficulty.entries.forEach { diff ->
            if (obj.has(diff.name)) {
                val d = obj.getJSONObject(diff.name)
                stats = stats.withUpdated(
                    diff,
                    DifficultyStats(
                        gamesWon  = d.getInt("won"),
                        bestTime  = d.getLong("best"),
                        totalTime = d.getLong("total")
                    )
                )
            }
        }
        return stats
    }
}