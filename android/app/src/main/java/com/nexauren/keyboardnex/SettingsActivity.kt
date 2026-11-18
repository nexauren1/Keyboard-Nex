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
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.setBackgroundColor(backgroundColor)
        render()
    }

    private fun render() {
        scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(28))
        }
        scroll.addView(content)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(text("‹", 34, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(54)))
        header.addView(text("Definições do teclado", 22, Color.WHITE, true).apply {
            setPadding(dp(8), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        content.addView(header)

        content.addView(text(
            "Personaliza a disposição, o comportamento das teclas e a resposta ao toque.",
            13, secondary
        ).apply { setPadding(dp(4), dp(2), dp(4), dp(18)) })

        section("DISPOSIÇÃO DO TECLADO")
        switchOption(
            "Linha de números",
            "Mostrar 1–0 acima das letras. Pode ocupar um pouco mais de espaço.",
            "number_row_enabled",
            true
        )
        switchOption(
            "Tecla de emojis",
            "Mostrar uma tecla de acesso rápido aos emojis na linha inferior.",
            "emoji_key_enabled",
            true
        ) { enabled ->
            if (enabled) prefs.edit().putBoolean("language_switch_key_enabled", false).apply()
        }
        switchOption(
            "Tecla de mudança de idioma",
            "Mostrar a tecla de globo para alternar entre teclados instalados.",
            "language_switch_key_enabled",
            false
        ) { enabled ->
            if (enabled) prefs.edit().putBoolean("emoji_key_enabled", false).apply()
        }
        switchOption(
            "Tecla de vírgula",
            "Mostrar a vírgula ao lado da barra de espaço.",
            "comma_key_enabled",
            true
        )
        switchOption(
            "Tecla de ponto final",
            "Mostrar o ponto final na linha inferior.",
            "period_key_enabled",
            true
        )

        section("DIGITAÇÃO")
        switchOption(
            "Ponto com duplo espaço",
            "Tocar duas vezes na barra de espaço insere ponto e espaço.",
            "double_space_period_enabled",
            true
        )
        switchOption(
            "Números ao manter uma tecla",
            "Manter Q, W, E, R, T, Y, U, I, O ou P para escrever 1–0.",
            "long_press_symbols_enabled",
            true
        )
        choiceOption(
            "Altura do teclado",
            "keyboard_height",
            arrayOf("Compacta", "Confortável", "Alta"),
            arrayOf("compact", "normal", "tall"),
            "normal"
        )

        section("TOQUE E RESPOSTA")
        switchOption(
            "Vibração das teclas",
            "Vibrar ligeiramente ao tocar numa tecla.",
            "haptics_enabled",
            true
        )
        switchOption(
            "Som das teclas",
            "Reproduzir o som de tecla do sistema.",
            "sound_enabled",
            false
        )

        section("APARÊNCIA")
        option(
            "Tema do teclado",
            currentTheme() + " · Escolher cores e contraste"
        ) { chooseTheme() }

        section("PRIVACIDADE")
        val privacy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(cardColor, 17)
        }
        privacy.addView(text(
            "O conteúdo digitado permanece no dispositivo nesta versão. O histórico da área de transferência é local. Campos de palavra-passe e campos que desativam a aprendizagem são excluídos da captura.",
            13, secondary
        ))
        content.addView(privacy)

        content.addView(text("NEXAUREN  •  KEYBOARD NEX", 10, secondary, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(4))
        })
    }

    private fun section(title: String) {
        content.addView(text(title, 11, Color.rgb(177, 163, 255), true).apply {
            letterSpacing = 0.08f
            setPadding(dp(4), dp(18), 0, dp(8))
        })
    }

    private fun switchOption(
        title: String,
        summary: String,
        key: String,
        defaultValue: Boolean,
        onToggle: (Boolean) -> Unit = {}
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(12), dp(12))
            background = rounded(cardColor, 15)
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 0, dp(8), 0)
        }
        labels.addView(text(title, 15, Color.WHITE, true))
        labels.addView(text(summary, 12, secondary).apply {
            setPadding(0, dp(4), 0, 0)
        })
        row.addView(labels)
        val control = Switch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                onToggle(checked)
            }
        }
        row.addView(control)
        content.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(7) })
    }

    private fun choiceOption(
        title: String,
        key: String,
        labels: Array<String>,
        values: Array<String>,
        defaultValue: String
    ) {
        val selectedValue = prefs.getString(key, defaultValue) ?: defaultValue
        val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
        option(title, labels[selectedIndex] + " · Alterar") {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                    prefs.edit().putString(key, values[which]).apply()
                    dialog.dismiss()
                    render()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun option(title: String, summary: String, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(15))
            background = rounded(cardColor, 15)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animate().alpha(0.75f).setDuration(55).withEndAction {
                    animate().alpha(1f).setDuration(80).start()
                    action()
                }.start()
            }
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        labels.addView(text(title, 15, Color.WHITE, true))
        labels.addView(text(summary, 12, secondary).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(labels)
        row.addView(text("›", 25, secondary))
        content.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(7) })
    }

    private fun chooseTheme() {
        val themes = arrayOf("Neon Violet", "Midnight Blue", "Ocean Cyan", "Minimal Light")
        AlertDialog.Builder(this)
            .setTitle("Tema do teclado")
            .setSingleChoiceItems(themes, themes.indexOf(currentTheme()).coerceAtLeast(0)) { dialog, which ->
                prefs.edit().putString("theme_name", themes[which]).apply()
                dialog.dismiss()
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun currentTheme() =
        prefs.getString("theme_name", "Neon Violet") ?: "Neon Violet"

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
