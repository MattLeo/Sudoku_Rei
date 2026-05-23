package com.sudokurei

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import com.sudokurei.ui.SudokuScreen
import com.sudokurei.ui.theme.SudokuTheme
import com.sudokurei.viewmodel.SudokuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val vm: SudokuViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            SudokuTheme(themeMode = themeMode) {
                SudokuScreen(vm = vm)
            }
        }
    }
}