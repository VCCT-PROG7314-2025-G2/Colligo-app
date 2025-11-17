package com.varsitycollege.st10303285.colligoapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class LanguageActivity : AppCompatActivity() {

    private lateinit var group: RadioGroup
    private lateinit var rbEnglish: RadioButton
    private lateinit var rbAfrikaans: RadioButton
    private lateinit var btnApply: Button

    private val PREFS = "app_prefs"
    private val KEY_LANG = "pref_lang"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        group = findViewById(R.id.langGroup)
        rbEnglish = findViewById(R.id.rbEnglish)
        rbAfrikaans = findViewById(R.id.rbAfrikaans)
        btnApply = findViewById(R.id.btnApplyLang)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_LANG, "en") ?: "en"
        if (lang == "af") rbAfrikaans.isChecked = true else rbEnglish.isChecked = true

        btnApply.setOnClickListener {
            val selected = when (group.checkedRadioButtonId) {
                R.id.rbAfrikaans -> "af"
                else -> "en"
            }
            prefs.edit().putString(KEY_LANG, selected).apply()
            applyLocale(selected)

            // restart home so strings reload simply
            val i = Intent(this, HomeActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
            finish()
        }
    }

    private fun applyLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}