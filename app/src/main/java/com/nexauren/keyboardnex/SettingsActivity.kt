package com.nexauren.keyboardnex

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.nexauren.keyboardnex.clipboard.ClipboardStore

class SettingsActivity : Activity() {
    private val backgroundColor = Color.rgb(9, 11, 22)
    private val cardColor = Color.rgb(20, 23, 42)
    private val accent = Color.rgb(112, 79, 255)
    private val secondary = Color.rgb(161, 168, 194)
    private val prefs by lazy {
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
    }
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        render()
    }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(backgroundColor) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(25))
        }
        scroll.addView(content)
        setContentView(scroll)

        val back = text("‹", 32, Color.WHITE, true).apply {
            setOnClickListener { finish() }
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))

        content.addView(text("Definições do Keyboard Nex", 27, Color.WHITE, true).apply {
            setPadding(0, dp(20), 0, dp(24))
        })

        option("◎", "Idiomas", "Layout inicial QWERTY · pacotes de idioma em preparação") {
            AlertDialog.Builder(this)
                .setTitle("Idiomas e layouts")
                .setMessage("A primeira versão utiliza um layout QWERTY base. Os pacotes de idioma, dicionários, alternância multilingue e correção específica para Português, Inglês, Espanhol, Francês e Alemão serão adicionados na próxima etapa.")
                .setPositiveButton("Entendi", null)
                .show()
        }
        option("☷", "Preferências", "Altura do teclado, vibração e comportamento das teclas") {
            showPreferences()
        }
        option("◉", "Tema", currentTheme(), "Escolha um dos temas disponíveis") {
            chooseTheme()
        }
        option("A✓", "Correções e sugestões", "Motor de escrita em desenvolvimento") {
            featureInfo("Correções e sugestões", "Autocorreção, sugestões de palavras, capitalização automática, pontuação inteligente e dicionário pessoal serão integrados no motor de escrita.")
        }
        option("〰", "Escrita ao deslizar", "Em desenvolvimento") {
            featureInfo("Escrita ao deslizar", "O motor de digitação por gestos ainda não está integrado. Esta função será desenvolvida e testada separadamente para garantir precisão.")
        }
        option("🎙", "Digitação por voz", "Integração de ditado em desenvolvimento") {
            featureInfo("Digitação por voz", "O Keyboard Nex ainda não iniciou a captura de voz. A integração será opcional e dependerá do serviço de reconhecimento disponível no dispositivo.")
        }
        option("▤", "Área de transferência", "Histórico, seleção múltipla, fixar, editar e apagar") {
            openClipboardTest()
        }
        option("▦", "Dicionário pessoal", "Em desenvolvimento") {
            featureInfo("Dicionário pessoal", "A futura tela permitirá adicionar palavras, editar entradas, remover termos e criar atalhos de texto.")
        }

        content.addView(text("PRIVACIDADE", 11, Color.rgb(160, 151, 220), true).apply {
            letterSpacing = 0.1f
            setPadding(dp(2), dp(18), 0, dp(9))
        })
        val privacy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(cardColor, 18)
        }
        privacy.addView(text("O texto digitado não é enviado para servidores nesta versão. O histórico de clips é local, pode ser desativado e tem validade configurável. Campos de palavra-passe e campos que desativam aprendizagem são excluídos da captura.", 13, secondary))
        content.addView(privacy)

        content.addView(text("NEXAUREN  •  BUILDING DIGITAL PRODUCTS", 10, secondary, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })
    }

    private fun option(icon: String, title: String, summary: String, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = rounded(cardColor, 17)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animate().scaleX(0.99f).scaleY(0.99f).setDuration(55).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    action()
                }.start()
            }
        }
        row.addView(text(icon, 23, Color.rgb(189, 178, 255), true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(44), dp(48)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(8), 0, dp(6), 0)
        }
        labels.addView(text(title, 16, Color.WHITE))
        labels.addView(text(summary, 12, secondary).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(labels)
        row.addView(text("›", 24, secondary))
        content.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun showPreferences() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }
        val haptic = Switch(this).apply {
            text = "Vibração ao pressionar teclas"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("haptics_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("haptics_enabled", checked).apply()
            }
        }
        box.addView(haptic)

        val sound = Switch(this).apply {
            text = "Som das teclas"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("sound_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("sound_enabled", checked).apply()
            }
        }
        box.addView(sound)

        val currentHeight = prefs.getString("keyboard_height", "normal") ?: "normal"
        val heightLabels = arrayOf("Compacta", "Padrão", "Alta")
        val heightValues = arrayOf("compact", "normal", "tall")
        val selected = heightValues.indexOf(currentHeight).coerceAtLeast(0)
        val heightButton = TextView(this).apply {
            text = "Altura das teclas: " + heightLabels[selected] + "  ›"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, dp(15), 0, dp(9))
            setOnClickListener {
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Altura do teclado")
                    .setSingleChoiceItems(heightLabels, selected) { dialog, which ->
                        prefs.edit().putString("keyboard_height", heightValues[which]).apply()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        box.addView(heightButton)

        AlertDialog.Builder(this)
            .setTitle("Preferências")
            .setView(box)
            .setPositiveButton("Concluído", null)
            .show()
    }

    private fun chooseTheme() {
        val themes = arrayOf("Neon Violet", "Midnight Blue", "Ocean Cyan", "Minimal Light")
        AlertDialog.Builder(this)
            .setTitle("Tema do teclado")
            .setItems(themes) { _, which ->
                prefs.edit().putString("theme_name", themes[which]).apply()
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openClipboardTest() {
        startActivity(android.content.Intent(this, MainActivity::class.java))
    }

    private fun currentTheme() =
        prefs.getString("theme_name", "Neon Violet") ?: "Neon Violet"

    private fun featureInfo(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
