package com.nexauren.keyboardnex

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.nexauren.keyboardnex.clipboard.ClipboardStore

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
    }

    private val bg = Color.rgb(9, 11, 22)
    private val panel = Color.rgb(20, 23, 42)
    private val accent = Color.rgb(112, 79, 255)
    private val muted = Color.rgb(161, 168, 194)
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun showHome() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(content)
        setContentView(scroll)

        val eyebrow = text("NEXAUREN  /  ANDROID INPUT", 11, muted, true)
        eyebrow.letterSpacing = 0.12f
        content.addView(eyebrow)

        val title = text("Keyboard Nex", 30, Color.WHITE, true)
        title.setPadding(0, dp(8), 0, 0)
        content.addView(title)
        content.addView(text("O seu teclado. Do seu jeito.", 15, muted).apply {
            setPadding(0, dp(4), 0, dp(18))
        })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(intArrayOf(Color.rgb(35, 38, 91), Color.rgb(75, 30, 119)), 24)
        }
        hero.addView(text("PERSONALIZAÇÃO E CONTROLO", 11, Color.WHITE, true))
        hero.addView(text("Temas, escrita e área de transferência num só teclado.", 19, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(16))
        })
        hero.addView(actionButton("Ativar Keyboard Nex") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        hero.addView(actionButton("Selecionar Keyboard Nex") {
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        })
        content.addView(hero, marginParams(top = 4, bottom = 20))

        section("TESTAR")
        val testBox = EditText(this).apply {
            hint = "Toque aqui para testar o teclado"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 16f
            minLines = 2
            gravity = Gravity.TOP
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(intArrayOf(panel, panel), 18)
        }
        content.addView(testBox, marginParams(bottom = 18))

        section("ÁREA DE TRANSFERÊNCIA")
        val autoSave = Switch(this).apply {
            text = "Guardar automaticamente o texto copiado"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("auto_capture", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("auto_capture", checked).apply()
            }
        }
        val autoCard = card()
        autoCard.addView(autoSave)
        autoCard.addView(text("O histórico fica guardado localmente no dispositivo. Não enviamos os textos para servidores.", 12, muted))
        content.addView(autoCard, marginParams(bottom = 10))

        addOption(
            "Tempo para apagar textos",
            ttlLabel(),
            "Escolher a duração do histórico não fixado"
        ) { chooseTtl() }

        addOption(
            "Gerir área de transferência",
            "Fixar, editar e eliminar vários itens",
            "O painel de gestão também aparece dentro do teclado"
        ) {
            ClipboardStore.all(this)
            AlertDialog.Builder(this)
                .setTitle("Área de transferência")
                .setMessage("Abra qualquer campo de texto, toque no ícone de área de transferência no Keyboard Nex e faça a gestão dos seus itens.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        section("APARÊNCIA")
        addOption("Tema do teclado", prefs.getString("theme_name", "Neon Violet") ?: "Neon Violet", "Escolha uma aparência") {
            chooseTheme()
        }
        addOption("Altura do teclado", "Padrão", "Mais opções de tamanho serão adicionadas") {
            AlertDialog.Builder(this)
                .setTitle("Altura do teclado")
                .setItems(arrayOf("Compacta", "Padrão", "Alta")) { _, which ->
                    prefs.edit().putString("keyboard_height", arrayOf("compact", "normal", "tall")[which]).apply()
                }
                .show()
        }

        section("PRIVACIDADE")
        val privacy = card()
        privacy.addView(text("Keyboard Nex não pede acesso à Internet nesta versão inicial. O texto digitado não é recolhido para análise. O histórico da área de transferência é guardado localmente e pode ser apagado pelo utilizador.", 13, muted))
        content.addView(privacy, marginParams(bottom = 18))

        content.addView(text("NEXAUREN  •  BUILDING DIGITAL PRODUCTS", 10, muted, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        })

        content.alpha = 0f
        content.animate().alpha(1f).setDuration(260).start()
    }

    private fun chooseTtl() {
        val labels = arrayOf("5 minutos", "15 minutos", "30 minutos", "1 hora", "24 horas", "7 dias")
        val values = longArrayOf(
            5L * 60_000L, 15L * 60_000L, 30L * 60_000L,
            60L * 60_000L, 24L * 60L * 60_000L, 7L * 24L * 60L * 60_000L
        )
        AlertDialog.Builder(this)
            .setTitle("Apagar textos não fixados após")
            .setSingleChoiceItems(labels, values.indexOf(prefs.getLong("clip_ttl_ms", 60L * 60_000L))) { dialog, which ->
                prefs.edit().putLong("clip_ttl_ms", values[which]).apply()
                dialog.dismiss()
                showHome()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun chooseTheme() {
        val themes = arrayOf("Neon Violet", "Midnight Blue", "Ocean Cyan", "Minimal Light")
        AlertDialog.Builder(this)
            .setTitle("Tema do teclado")
            .setItems(themes) { _, which ->
                prefs.edit().putString("theme_name", themes[which]).apply()
                showHome()
            }
            .show()
    }

    private fun ttlLabel(): String {
        val value = prefs.getLong("clip_ttl_ms", 60L * 60_000L)
        return when (value) {
            5L * 60_000L -> "5 minutos"
            15L * 60_000L -> "15 minutos"
            30L * 60_000L -> "30 minutos"
            24L * 60L * 60_000L -> "24 horas"
            7L * 24L * 60L * 60_000L -> "7 dias"
            else -> "1 hora"
        }
    }

    private fun addOption(title: String, value: String, subtitle: String, action: () -> Unit) {
        val box = card().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setOnClickListener(action)
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        labels.addView(text(title, 15, Color.WHITE, true))
        labels.addView(text(subtitle, 12, muted).apply { setPadding(0, dp(4), 0, 0) })
        box.addView(labels)
        box.addView(text(value + "  ›", 12, Color.rgb(174, 159, 255), true))
        content.addView(box, marginParams(bottom = 9))
    }

    private fun section(title: String) {
        content.addView(text(title, 11, Color.rgb(160, 151, 220), true).apply {
            letterSpacing = 0.1f
            setPadding(dp(2), dp(12), 0, dp(9))
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(intArrayOf(panel, panel), 18)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = rounded(intArrayOf(accent, Color.rgb(77, 45, 223)), 16)
            setOnClickListener {
                it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    action()
                }.start()
            }
        }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun rounded(colors: IntArray, radius: Int) = GradientDrawable(
        if (colors.size > 1) GradientDrawable.Orientation.TL_BR else GradientDrawable.Orientation.LEFT_RIGHT,
        colors
    ).apply { cornerRadius = dp(radius).toFloat() }

    private fun marginParams(bottom: Int = 0, top: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (top > 0) topMargin = dp(top)
            if (bottom > 0) bottomMargin = dp(bottom)
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
