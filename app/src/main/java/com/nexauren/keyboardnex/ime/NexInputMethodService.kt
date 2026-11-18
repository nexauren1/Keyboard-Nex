package com.nexauren.keyboardnex.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.text.InputType
import android.inputmethodservice.InputMethodService
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexauren.keyboardnex.MainActivity
import com.nexauren.keyboardnex.clipboard.ClipboardStore

class NexInputMethodService : InputMethodService() {
    private val keyColor: Int get() = if (themeName() == "Minimal Light") Color.rgb(244, 246, 252) else Color.rgb(31, 35, 59)
    private val textColor: Int get() = if (themeName() == "Minimal Light") Color.rgb(28, 32, 49) else Color.rgb(245, 246, 255)
    private val muted: Int get() = if (themeName() == "Minimal Light") Color.rgb(91, 99, 124) else Color.rgb(169, 176, 204)
    private var shifted = false
    private var numberMode = false
    private var root: LinearLayout? = null
    private val selectedClipIds = linkedSetOf<String>()
    private lateinit var clipboardManager: ClipboardManager
    private var listenerRegistered = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureCurrentClipboard()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateInputView(): View {
        buildKeyboard()
        return root ?: LinearLayout(this)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!listenerRegistered) {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            listenerRegistered = true
        }
        captureCurrentClipboard()
        showKeyboard()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (listenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            listenerRegistered = false
        }
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        if (listenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            listenerRegistered = false
        }
        super.onDestroy()
    }

    private fun isPrivateEditor(): Boolean {
        val info = currentInputEditorInfo ?: return false
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val passwordField =
            (inputClass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            )) ||
            (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val noLearning = info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        return passwordField || noLearning
    }

    private fun captureCurrentClipboard() {
        if (isPrivateEditor()) return
        if (!ClipboardStore.isAutoCaptureEnabled(this)) return
        try {
            if (!clipboardManager.hasPrimaryClip()) return
            val clip = clipboardManager.primaryClip ?: return
            if (Build.VERSION.SDK_INT >= 33) {
                val sensitive = clip.description.extras
                    ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
                if (sensitive) return
            }
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            if (text.length > 5000) return
            ClipboardStore.capture(this, text)
        } catch (_: SecurityException) {
            // Clipboard access can be restricted by Android version or device policy.
        } catch (_: RuntimeException) {
            // Ignore clipboard provider failures without interrupting typing.
        }
    }

    private fun buildKeyboard() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(7))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(themeSurface(), themeBase())
            )
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val host = root ?: return
        host.animate().cancel()
        host.alpha = 0f
        host.removeAllViews()
        host.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(themeSurface(), themeBase())
        )

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addTool(toolbar, "▤", "Área de transferência") { showClipboardPanel() }
        addTool(toolbar, "☺", "Emojis") { showEmojiPanel() }
        addTool(toolbar, "Aa", "Ferramentas") { showToolsPanel() }
        addTool(toolbar, "🎨", "Temas") { showThemePanel() }
        addTool(toolbar, "⚙", "Definições") { openSettings() }
        host.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
        ))

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
        }
        suggestions.addView(label("Keyboard Nex", 12, muted, true), LinearLayout.LayoutParams(0, dp(28), 1f))
        suggestions.addView(label("QWERTY", 11, muted), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(28)
        ))
        host.addView(suggestions)

        val rows = if (numberMode) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
                listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫"),
                listOf("ABC", "☺", ",", "ESPAÇO", ".", "↵")
            )
        } else {
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
                listOf("?123", "☺", ",", "ESPAÇO", ".", "↵")
            )
        }

        rows.forEachIndexed { index, values ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val height = when (prefsHeight()) {
                "compact" -> 42
                "tall" -> 55
                else -> 48
            }
            val rowHeight = if (index == 3) height + 3 else height
            values.forEach { value ->
                val weight = when (value) {
                    "ESPAÇO" -> 3.1f
                    "?123", "ABC" -> 1.4f
                    "⇧", "⌫" -> 1.35f
                    "↵" -> 1.45f
                    else -> 1f
                }
                val button = keyButton(displayKey(value), isSpecial(value))
                button.layoutParams = LinearLayout.LayoutParams(0, dp(rowHeight), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                }
                button.setOnClickListener {
                    animatePress(button)
                    button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    handleKey(value)
                }
                if (value == "⌫") {
                    button.setOnLongClickListener {
                        currentInputConnection?.deleteSurroundingText(30, 0)
                        true
                    }
                }
                row.addView(button)
            }
            host.addView(row)
        }
        host.animate().alpha(1f).setDuration(150).start()
    }

    private fun handleKey(value: String) {
        val connection = currentInputConnection ?: return
        when (value) {
            "⇧" -> {
                shifted = !shifted
                showKeyboard()
            }
            "⌫" -> deleteBeforeCursor(connection)
            "?123" -> {
                numberMode = true
                showKeyboard()
            }
            "ABC" -> {
                numberMode = false
                showKeyboard()
            }
            "☺" -> showEmojiPanel()
            "ESPAÇO" -> {
                connection.commitText(" ", 1)
                shifted = false
            }
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && sendDefaultEditorAction(true)) return
                connection.commitText("\n", 1)
            }
            else -> {
                val output = if (shifted) value.uppercase() else value
                connection.commitText(output, 1)
                if (shifted) {
                    shifted = false
                    showKeyboard()
                }
            }
        }
    }

    private fun deleteBeforeCursor(connection: InputConnection) {
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val count = if (before.length >= 2 &&
            Character.isSurrogatePair(before[before.length - 2], before[before.length - 1])) 2 else 1
        connection.deleteSurroundingText(count, 0)
    }

    private fun showClipboardPanel() {
        val host = root ?: return
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(5))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Área de transferência", 16, textColor, true), LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)

        val actions = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow.addView(smallButton("Marcar tudo") {
            val allIds = ClipboardStore.all(this@NexInputMethodService).map { it.id }
            if (selectedClipIds.size == allIds.size) selectedClipIds.clear()
            else selectedClipIds.addAll(allIds)
            showClipboardPanel()
        })
        actionRow.addView(smallButton("Fixar") {
            if (selectedClipIds.isNotEmpty()) {
                val all = ClipboardStore.all(this@NexInputMethodService)
                val selected = all.filter { it.id in selectedClipIds }
                val shouldPin = selected.any { !it.pinned }
                ClipboardStore.setPinned(this@NexInputMethodService, selectedClipIds, shouldPin)
                selectedClipIds.clear()
                showClipboardPanel()
            }
        })
        actionRow.addView(smallButton("Eliminar") {
            if (selectedClipIds.isNotEmpty()) {
                ClipboardStore.delete(this@NexInputMethodService, selectedClipIds)
                selectedClipIds.clear()
                showClipboardPanel()
            }
        })
        actions.addView(actionRow)
        panel.addView(actions)

        val list = ScrollView(this)
        val items = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(4))
        }
        val clips = ClipboardStore.all(this)
        selectedClipIds.retainAll(clips.map { it.id }.toSet())

        if (clips.isEmpty()) {
            items.addView(label("Ainda não há textos guardados. Copie algum texto e volte a abrir o teclado.", 13, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(24), dp(18), dp(24))
            })
        } else {
            clips.forEach { clip ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(7), dp(7), dp(7), dp(7))
                    background = rounded(keyColor, 14)
                }
                val check = CheckBox(this).apply {
                    buttonTintList = ColorStateList.valueOf(themeAccent())
                    isChecked = clip.id in selectedClipIds
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedClipIds.add(clip.id) else selectedClipIds.remove(clip.id)
                    }
                }
                card.addView(check)
                val body = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(dp(4), 0, dp(4), 0)
                    isClickable = true
                    setOnClickListener {
                        currentInputConnection?.commitText(clip.text, 1)
                        selectedClipIds.clear()
                        showKeyboard()
                    }
                }
                body.addView(label(clip.text, 13, textColor, true).apply {
                    maxLines = 3
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                body.addView(label(
                    if (clip.pinned) "Fixado" else expiryLabel(clip.expiresAt),
                    10,
                    if (clip.pinned) themeAccent() else muted
                ).apply { setPadding(0, dp(3), 0, 0) })
                card.addView(body)
                card.addView(smallButton("✎") { showEditPanel(clip.id, clip.text) })
                card.addView(smallButton(if (clip.pinned) "📌" else "＋") {
                    ClipboardStore.setPinned(this@NexInputMethodService, setOf(clip.id), !clip.pinned)
                    showClipboardPanel()
                })
                items.addView(card, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(5) })
            }
        }

        list.addView(items)
        panel.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        panel.addView(label("Os itens não fixados são apagados após o tempo definido nas definições.", 10, muted).apply {
            setPadding(dp(3), dp(4), dp(3), dp(2))
        })
        transitionPanel(panel)
    }

    private fun showEditPanel(id: String, original: String) {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        panel.addView(label("Editar texto guardado", 16, textColor, true))
        val editor = EditText(this).apply {
            setText(original)
            setTextColor(textColor)
            setHintTextColor(muted)
            textSize = 14f
            gravity = Gravity.TOP
            minLines = 3
            maxLines = 6
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = rounded(keyColor, 12)
        }
        panel.addView(editor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(8); bottomMargin = dp(8) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(smallButton("Cancelar") { showClipboardPanel() })
        buttons.addView(smallButton("Guardar") {
            ClipboardStore.updateText(this@NexInputMethodService, id, editor.text.toString())
            showClipboardPanel()
        })
        panel.addView(buttons)
        transitionPanel(panel)
    }

    private fun showEmojiPanel() {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Emojis", 16, textColor, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)

        val emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "🙂", "🙃", "😉", "😍", "🥰", "😘", "😎",
            "😭", "😢", "😡", "🤔", "🤗", "🙏", "👏", "👍",
            "❤️", "💙", "💜", "🔥", "✨", "🎉", "✅", "🚀",
            "🌍", "☀️", "🌙", "⭐", "⚡", "🎧", "📱", "💡"
        )
        val scroller = ScrollView(this)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        emojis.chunked(8).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { emoji ->
                val button = keyButton(emoji, false).apply {
                    textSize = 22f
                    layoutParams = LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    setOnClickListener { currentInputConnection?.commitText(emoji, 1) }
                }
                row.addView(button)
            }
            grid.addView(row)
        }
        scroller.addView(grid)
        panel.addView(scroller, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        host.addView(panel)
    }

    private fun showToolsPanel() {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Ferramentas rápidas", 16, textColor, true), LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)
        panel.addView(smallButton("Abrir definições do Keyboard Nex") { openSettings() })
        panel.addView(label("Planeado: snippets, tradução, edição de texto, cursor por gestos, pesquisa de GIFs e ditado por voz.", 12, muted).apply {
            setPadding(dp(4), dp(10), dp(4), dp(4))
        })
        host.addView(panel)
    }

    private fun showThemePanel() {
        val host = root ?: return
        host.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("Temas", 16, textColor, true), LinearLayout.LayoutParams(0, dp(40), 1f))
        header.addView(smallButton("Voltar") { showKeyboard() })
        panel.addView(header)
        listOf("Neon Violet", "Midnight Blue", "Ocean Cyan", "Minimal Light").forEach { theme ->
            panel.addView(smallButton(theme) {
                getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
                    .edit().putString("theme_name", theme).apply()
                showKeyboard()
            })
        }
        host.addView(panel)
    }


    private fun transitionPanel(panel: View) {
        val host = root ?: return
        host.animate().cancel()
        host.animate().alpha(0f).setDuration(85).withEndAction {
            host.removeAllViews()
            host.addView(
                panel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
            host.alpha = 0f
            host.animate().alpha(1f).setDuration(165).start()
        }.start()
    }

    private fun openSettings() {
        val intent = android.content.Intent(this, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun addTool(row: LinearLayout, symbol: String, description: String, action: () -> Unit) {
        val item = label(symbol, 20, textColor, true).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animatePress(this)
                action()
            }
        }
        row.addView(item, LinearLayout.LayoutParams(0, dp(40), 1f))
    }

    private fun smallButton(value: String, action: () -> Unit) =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = rounded(themeAccent(), 11)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animatePress(this)
                action()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)
            ).apply { setMargins(dp(3), dp(2), dp(3), dp(2)) }
        }

    private fun keyButton(value: String, special: Boolean) =
        TextView(this).apply {
            text = value
            textSize = if (value == "ESPAÇO") 11f else 17f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = rounded(if (special) themeAccent() else keyColor, 9)
            isClickable = true
            isFocusable = true
        }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) =
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

    private fun displayKey(value: String): String = when (value) {
        "ESPAÇO" -> "Espaço"
        else -> if (shifted && value.length == 1 && value[0].isLetter()) value.uppercase() else value
    }

    private fun isSpecial(value: String) =
        value in setOf("⇧", "⌫", "?123", "ABC", "↵", "☺", "ESPAÇO")

    private fun animatePress(view: View) {
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(55).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(85).start()
        }.start()
    }

    private fun expiryLabel(expiresAt: Long?): String {
        if (expiresAt == null) return "Sem expiração"
        val remaining = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remaining / 60_000L).coerceAtLeast(1L)
        return if (minutes < 60L) "Expira em " + minutes + " min" else "Expira em " + (minutes / 60L) + " h"
    }

    private fun prefsHeight(): String =
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .getString("keyboard_height", "normal") ?: "normal"

    private fun themeName(): String =
        getSharedPreferences(ClipboardStore.SETTINGS, Context.MODE_PRIVATE)
            .getString("theme_name", "Neon Violet") ?: "Neon Violet"

    private fun themeAccent(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(52, 96, 212)
        "Ocean Cyan" -> Color.rgb(0, 157, 183)
        "Minimal Light" -> Color.rgb(104, 111, 135)
        else -> Color.rgb(105, 76, 245)
    }

    private fun themeSurface(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(15, 24, 50)
        "Ocean Cyan" -> Color.rgb(8, 39, 50)
        "Minimal Light" -> Color.rgb(226, 230, 243)
        else -> Color.rgb(27, 20, 62)
    }

    private fun themeBase(): Int = when (themeName()) {
        "Midnight Blue" -> Color.rgb(8, 14, 32)
        "Ocean Cyan" -> Color.rgb(5, 22, 31)
        "Minimal Light" -> Color.rgb(202, 209, 228)
        else -> Color.rgb(12, 10, 31)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
