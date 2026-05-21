package com.sudokurei.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sudokurei.game.CellState
import com.sudokurei.game.Difficulty
import com.sudokurei.game.GameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sudoku_game")

class GameRepository(private val context: Context) {

    private val GAME_KEY = stringPreferencesKey("game_state")

    suspend fun save(state: GameState, showMenu: Boolean) {
        val json = serialize(state, showMenu)
        context.dataStore.edit { it[GAME_KEY] = json }
    }

    suspend fun load(): Pair<GameState, Boolean>? {
        val json = context.dataStore.data.map { it[GAME_KEY] }.first() ?: return null
        return try {
            deserialize(json)
        } catch (_: Exception) {
            null // corrupt data — start fresh
        }
    }

    // ── Serialization ──────────────────────────────────────────────────────

    private fun serialize(state: GameState, showMenu: Boolean): String {
        val obj = JSONObject()

        val cellsArr = JSONArray()
        state.cells.forEach { cell ->
            val c = JSONObject()
            c.put("v", cell.value)
            c.put("g", cell.isGiven)
            c.put("e", cell.isError)
            val notes = JSONArray()
            cell.notes.forEach { notes.put(it) }
            c.put("n", notes)
            cellsArr.put(c)
        }
        obj.put("cells", cellsArr)

        val sol = JSONArray()
        state.solution.forEach { sol.put(it) }
        obj.put("solution", sol)

        obj.put("difficulty",     state.difficulty.name)
        obj.put("selectedCell",   state.selectedCell)
        obj.put("hintsUsed",      state.hintsUsed)
        obj.put("maxHints",       state.maxHints)
        obj.put("isComplete",     state.isComplete)
        obj.put("elapsedSeconds", state.elapsedSeconds)
        obj.put("isNotesMode",    state.isNotesMode)
        obj.put("showErrors",     state.showErrors)
        obj.put("showMenu",       showMenu)

        return obj.toString()
    }

    private fun deserialize(json: String): Pair<GameState, Boolean> {
        val obj = JSONObject(json)

        val cellsArr = obj.getJSONArray("cells")
        val cells = (0 until cellsArr.length()).map { i ->
            val c = cellsArr.getJSONObject(i)
            val notesArr = c.getJSONArray("n")
            val notes = (0 until notesArr.length()).map { notesArr.getInt(it) }.toSet()
            CellState(
                value   = c.getInt("v"),
                isGiven = c.getBoolean("g"),
                isError = c.getBoolean("e"),
                notes   = notes
            )
        }

        val solArr = obj.getJSONArray("solution")
        val solution = IntArray(solArr.length()) { solArr.getInt(it) }

        val state = GameState(
            cells          = cells,
            solution       = solution,
            difficulty     = Difficulty.valueOf(obj.getString("difficulty")),
            selectedCell   = obj.getInt("selectedCell"),
            hintsUsed      = obj.getInt("hintsUsed"),
            maxHints       = obj.getInt("maxHints"),
            isComplete     = obj.getBoolean("isComplete"),
            elapsedSeconds = obj.getLong("elapsedSeconds"),
            isNotesMode    = obj.getBoolean("isNotesMode"),
            showErrors     = obj.getBoolean("showErrors")
        )

        return Pair(state, obj.getBoolean("showMenu"))
    }
}