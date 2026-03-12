package com.saas.apkeditorplus

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.util.*

data class AppInfo(
    val name: String,
    val packageName: String,
    val sourceDir: String,
    var icon: Drawable? = null
)

class AppAdapter(private val context: Context) : BaseAdapter() {

    private var apps: List<AppInfo> = listOf()
    private val packageManager: PackageManager = context.packageManager
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val iconCache = LruCache<String, Drawable>(64)

    fun setApps(newApps: List<AppInfo>) {
        apps = newApps.sortedBy { it.name.lowercase(Locale.getDefault()) }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): AppInfo = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.item_applist, parent, false)
        val iconView: ImageView = view.findViewById(R.id.app_icon)
        val nameView: TextView = view.findViewById(R.id.app_name)
        val packageView: TextView = view.findViewById(R.id.app_desc1)

        val app = getItem(position)
        nameView.text = app.name
        packageView.text = app.packageName

        // Carregamento de ícone com cache
        val cachedIcon = iconCache.get(app.packageName)
        if (cachedIcon != null) {
            iconView.setImageDrawable(cachedIcon)
        } else {
            // No mundo ideal, isso seria em uma thread separada (Glide/Coil), 
            // mas para paridade simples com o original usaremos o cache direto.
            try {
                val icon = packageManager.getApplicationIcon(app.packageName)
                iconCache.put(app.packageName, icon)
                iconView.setImageDrawable(icon)
            } catch (e: Exception) {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }

        return view
    }
}
