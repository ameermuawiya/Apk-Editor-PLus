package com.saas.apkeditorplus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.saas.apkeditorplus.utils.AxmlDecoder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class AxmlEditActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var apkPath: String
    private val xmlFiles = mutableListOf<String>()
    private val modifiedFiles = mutableMapOf<String, String>() // ZipEntry -> Path to modified temp file

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_axmledit)

        apkPath = intent.getStringExtra("apkPath") ?: ""
        if (apkPath.isEmpty()) {
            finish()
        }

        listView = findViewById(R.id.files_list)
        progressBar = findViewById(R.id.progress_bar)
        
        loadApkInfo()
        loadXmlFiles()

        findViewById<Button>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_save).setOnClickListener { startApkCreate() }
    }

    private fun loadApkInfo() {
        try {
            val pm = packageManager
            val info = pm.getPackageArchiveInfo(apkPath, 0)
            if (info != null) {
                info.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = apkPath
                    appInfo.publicSourceDir = apkPath
                    val label = appInfo.loadLabel(pm).toString()
                    val icon = appInfo.loadIcon(pm)
                    
                    findViewById<TextView>(R.id.apk_label).text = label
                    findViewById<ImageView>(R.id.apk_icon).setImageDrawable(icon)
                    findViewById<TextView>(R.id.tv_filepath).text = apkPath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            findViewById<TextView>(R.id.tv_filepath).text = apkPath
        }
    }

    private fun loadXmlFiles() {
        progressBar.visibility = View.VISIBLE
        Thread {
            try {
                val zipFile = ZipFile(apkPath)
                val entries = zipFile.entries()
                xmlFiles.clear()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".xml") && !entry.isDirectory) {
                        xmlFiles.add(entry.name)
                    }
                }
                xmlFiles.sort()
                zipFile.close()
                runOnUiThread {
                    updateList()
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Erro ao carregar arquivos: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun updateList() {
        listView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = xmlFiles.size
            override fun getItem(position: Int): Any = xmlFiles[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@AxmlEditActivity)
                    .inflate(R.layout.item_zipfile, parent, false)
                
                val textView = view.findViewById<TextView>(R.id.filename)
                val iconView = view.findViewById<ImageView>(R.id.file_icon)
                
                textView.text = xmlFiles[position]
                iconView.setImageResource(R.drawable.ic_file_xml)
                
                view.setOnClickListener {
                    openXmlFile(xmlFiles[position])
                }
                return view
            }
        }
    }

    private fun openXmlFile(entryName: String) {
        progressBar.visibility = View.VISIBLE
        Thread {
            try {
                val zipFile = ZipFile(apkPath)
                val entry = zipFile.getEntry(entryName)
                val inputStream = zipFile.getInputStream(entry)
                
                val tempDir = File(cacheDir, "xml_edit")
                if (!tempDir.exists()) tempDir.mkdirs()
                val tempFile = File(tempDir, entryName.replace("/", "_"))
                
                val outputStream = FileOutputStream(tempFile)
                
                // Tenta decodificar como AXML
                val decoder = AxmlDecoder()
                val success = decoder.decode(inputStream, outputStream)
                
                zipFile.close()
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (success) {
                        val intent = Intent(this, EditorActivity::class.java)
                        intent.putExtra("filePath", tempFile.absolutePath)
                        intent.putExtra("fileName", entryName)
                        startActivityForResult(intent, 100)
                        
                        modifiedFiles[entryName] = tempFile.absolutePath
                    } else {
                        Toast.makeText(this, "Falha ao decodificar arquivo", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            findViewById<Button>(R.id.btn_save).visibility = View.VISIBLE
        }
    }

    private fun startApkCreate() {
        val intent = Intent(this, ApkCreateActivity::class.java)
        intent.putExtra("apkPath", apkPath)
        val bundle = Bundle()
        for ((key, value) in modifiedFiles) {
            bundle.putString(key, value)
        }
        intent.putExtra("modifiedFiles", bundle)
        startActivity(intent)
        finish()
    }
}
