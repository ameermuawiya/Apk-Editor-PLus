package com.saas.apkeditorplus

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class EditModeDialog(
    context: Context,
    private val apkPath: String,
    private val onModeSelected: (mode: Int, path: String) -> Unit
) : Dialog(context), View.OnClickListener {

    init {
        requestWindowFeature(1) // Window.FEATURE_NO_TITLE
        val view = LayoutInflater.from(context).inflate(R.layout.dlg_editmode, null as ViewGroup?)
        setContentView(view)

        view.findViewById<TextView>(R.id.simple_edit).setOnClickListener(this)
        view.findViewById<TextView>(R.id.full_edit).setOnClickListener(this)
        view.findViewById<TextView>(R.id.common_edit).setOnClickListener(this)
        view.findViewById<TextView>(R.id.xml_edit).setOnClickListener(this)
        
        // Se houver um item de edição de dados, pode ser configurado aqui
        val dataEdit = view.findViewById<TextView>(R.id.data_edit)
        dataEdit?.visibility = View.GONE 
    }

    override fun onClick(v: View) {
        val mode = when (v.id) {
            R.id.full_edit -> 0
            R.id.simple_edit -> 1
            R.id.common_edit -> 2
            R.id.xml_edit -> 4
            else -> -1
        }
        
        if (mode != -1) {
            onModeSelected(mode, apkPath)
        }
        dismiss()
    }
}
