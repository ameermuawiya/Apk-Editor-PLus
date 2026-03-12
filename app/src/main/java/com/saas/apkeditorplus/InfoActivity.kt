package com.saas.apkeditorplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.net.Uri
import android.view.View

class InfoActivity : BaseActivity() {

    override fun shouldHideActionBar(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        
        setupToolbar()

        findViewById<View>(R.id.dev_github_link)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/FabioSilva11/"))
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            val inflater = layoutInflater
            val toolbarView = inflater.inflate(R.layout.mtrl_toolbar, null)
            val params = androidx.appcompat.app.ActionBar.LayoutParams(
                androidx.appcompat.app.ActionBar.LayoutParams.WRAP_CONTENT,
                androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT,
                android.view.Gravity.CENTER
            )
            
            val titleView = toolbarView.findViewById<android.widget.TextView>(android.R.id.title)
            titleView?.text = "Desenvolvedor"
            
            actionBar.setCustomView(toolbarView, params)
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
