package com.example.roteiro12
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.example.roteiro12.presentation.*
import com.example.roteiro12.ui.theme.Roteiro12Theme
@AndroidEntryPoint class MainActivity: ComponentActivity() {
 private val vm: AcervoViewModel by viewModels()
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { Roteiro12Theme { AcervoScreen(vm) } } }
}
