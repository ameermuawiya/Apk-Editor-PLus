package com.saas.apkeditorplus

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SlidingDrawer
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorActivity : BaseActivity() {

    private lateinit var editor: CodeEditor
    private lateinit var fileNameText: TextView
    private lateinit var saveBtn: ImageView
    private lateinit var openFindBtn: ImageView
    private lateinit var slidingDrawer: SlidingDrawer
    private lateinit var findEdit: EditText
    private lateinit var replaceEdit: EditText
    private lateinit var findBtn: ImageButton
    private lateinit var replaceBtn: ImageButton
    private lateinit var ignoreCaseCheck: ToggleButton
    private lateinit var regexpCheck: ToggleButton

    private var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editorutil_main)

        filePath = intent.getStringExtra("filePath")
        val fileName = intent.getStringExtra("fileName") ?: "Editor"

        editor = findViewById(R.id.editor)
        fileNameText = findViewById(R.id.filename)
        saveBtn = findViewById(R.id.saveBtn)
        openFindBtn = findViewById(R.id.openFindBtn)
        slidingDrawer = findViewById(R.id.sliding_drawer)
        findEdit = findViewById(R.id.findEdit)
        replaceEdit = findViewById(R.id.replaceEdit)
        findBtn = findViewById(R.id.findBtn)
        replaceBtn = findViewById(R.id.replaceBtn)
        ignoreCaseCheck = findViewById(R.id.checkBoxIgnoreCase)
        regexpCheck = findViewById(R.id.checkBoxRegexp)

        fileNameText.text = fileName

        setupEditor()
        loadFile()

        saveBtn.setOnClickListener { saveFile() }
        openFindBtn.setOnClickListener {
            if (slidingDrawer.isOpened) {
                slidingDrawer.animateClose()
            } else {
                slidingDrawer.animateOpen()
                slidingDrawer.visibility = android.view.View.VISIBLE
            }
        }

        findBtn.setOnClickListener { findNext() }
        replaceBtn.setOnClickListener { replaceAll() }
    }

    private fun setupEditor() {
        try {
            // Configurações básicas do Sora Editor
            // Removido setOverScrollEnabled para evitar erro de compilação entre versões
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFile() {
        filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                editor.setText(file.readText())
            }
        }
    }

    private fun findNext() {
        val query = findEdit.text.toString()
        if (query.isEmpty()) return

        try {
            val text = editor.text.toString()
            val ignoreCase = ignoreCaseCheck.isChecked
            val isRegexp = regexpCheck.isChecked
            
            val options = mutableSetOf<RegexOption>()
            if (ignoreCase) options.add(RegexOption.IGNORE_CASE)
            
            val pattern = if (isRegexp) query else Regex.escape(query)
            val regex = Regex(pattern, options)
            
            // Pega a posição atual do cursor para começar a busca
            val startIndex = editor.cursor.right
            
            var match = regex.find(text, startIndex)
            if (match == null) {
                // Se não encontrou do cursor pra frente, tenta do início (wrap-around)
                match = regex.find(text, 0)
            }
            
            if (match != null) {
                editor.setSelection(match.range.first, match.range.last + 1)
            } else {
                Toast.makeText(this, "Nenhuma correspondência encontrada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro na busca: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceAll() {
        val query = findEdit.text.toString()
        val replaceText = replaceEdit.text.toString()
        if (query.isEmpty()) return

        try {
            val text = editor.text.toString()
            val ignoreCase = ignoreCaseCheck.isChecked
            val isRegexp = regexpCheck.isChecked
            
            val options = mutableSetOf<RegexOption>()
            if (ignoreCase) options.add(RegexOption.IGNORE_CASE)
            
            val pattern = if (isRegexp) query else Regex.escape(query)
            val regex = Regex(pattern, options)
            
            val newText = text.replace(regex, replaceText)
            
            if (newText != text) {
                editor.setText(newText)
                Toast.makeText(this, "Substituições concluídas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Nenhuma correspondência encontrada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao substituir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFile() {
        filePath?.let { path ->
            try {
                File(path).writeText(editor.getText().toString())
                Toast.makeText(this, "Arquivo salvo com sucesso!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
