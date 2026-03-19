package com.musicnotation.editor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.musicnotation.editor.R
import com.musicnotation.editor.databinding.ActivityMainBinding
import com.musicnotation.editor.ui.editor.ScoreEditorFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ScoreEditorFragment())
                .commit()
        }
    }
}
