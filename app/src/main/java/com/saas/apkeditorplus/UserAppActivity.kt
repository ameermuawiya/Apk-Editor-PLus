package com.saas.apkeditorplus

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class UserAppActivity : BaseActivity(), View.OnClickListener, AdapterView.OnItemClickListener {

    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEdit: EditText
    private lateinit var adapter: AppAdapter
    private var showSystemApps = false
    private var allApps: List<AppInfo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applist)

        // Inicializa as views
        listView = findViewById(R.id.application_list)
        progressBar = findViewById(R.id.progress_bar)
        searchEdit = findViewById(R.id.et_keyword)
        
        updateTitle()

        adapter = AppAdapter(this)
        listView.adapter = adapter
        listView.onItemClickListener = this

        findViewById<View>(R.id.btn_close).setOnClickListener(this)
        findViewById<View>(R.id.btn_search).setOnClickListener(this)
        findViewById<View>(R.id.menu_more).setOnClickListener(this)
        
        // Configura a busca em tempo real
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
    }

    private fun updateTitle() {
        val titleText = findViewById<TextView>(R.id.apptype)
        titleText.text = "Select an application"
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        thread {
            val pm = packageManager
            // Use 0 como flag para ser mais rápido e evitar problemas de visibilidade se QUERY_ALL_PACKAGES estiver presente
            val installedApps = pm.getInstalledApplications(0)
            
            val appList = mutableListOf<AppInfo>()
            for (app in installedApps) {
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (showSystemApps == isSystem) {
                    appList.add(
                        AppInfo(
                            app.loadLabel(pm).toString(),
                            app.packageName,
                            app.publicSourceDir
                        )
                    )
                }
            }
            
            // Ordena os apps por nome
            appList.sortBy { it.name.lowercase() }
            
            allApps = appList
            
            runOnUiThread {
                progressBar.visibility = View.GONE
                adapter.setApps(allApps)
                searchEdit.isEnabled = true
                updateTitle()
            }
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
        adapter.setApps(filtered)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val app = adapter.getItem(position)
        showEditModeDialog(app.sourceDir)
    }

    private fun showEditModeDialog(path: String) {
        val dialog = EditModeDialog(this, path) { mode, apkPath ->
            // Inicia a atividade de edição correspondente
            startEditActivity(mode, apkPath)
        }
        dialog.show()
    }

    private fun startEditActivity(mode: Int, path: String) {
        val intent = when (mode) {
            1 -> android.content.Intent(this, SimpleEditActivity::class.java)
            2 -> android.content.Intent(this, CommonEditActivity::class.java)
            4 -> android.content.Intent(this, AxmlEditActivity::class.java)
            else -> null
        }

        intent?.let {
            it.putExtra("apkPath", path)
            startActivity(it)
        } ?: Toast.makeText(this, "Modo de edição não suportado ainda", Toast.LENGTH_SHORT).show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_close -> finish()
            R.id.btn_search -> {
                val query = searchEdit.text.toString()
                filterApps(query)
            }
            R.id.menu_more -> {
                showMenu(v)
            }
        }
    }

    private fun showMenu(v: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, v)
        popup.menu.add(0, 1, 0, "User App")
        popup.menu.add(0, 2, 0, "System App")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showSystemApps = false
                    loadApps()
                    true
                }
                2 -> {
                    showSystemApps = true
                    loadApps()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
