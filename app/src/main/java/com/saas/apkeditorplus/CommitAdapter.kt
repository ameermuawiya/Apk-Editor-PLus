package com.saas.apkeditorplus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CommitAdapter(private val context: Context) : BaseAdapter() {

    private var commits: List<GitHubCommit> = listOf()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val executor = Executors.newFixedThreadPool(4)
    private val handler = Handler(Looper.getMainLooper())
    private val avatarCache = mutableMapOf<String, Bitmap>()

    fun setCommits(newCommits: List<GitHubCommit>) {
        commits = newCommits
        notifyDataSetChanged()
    }

    override fun getCount(): Int = commits.size

    override fun getItem(position: Int): GitHubCommit = commits[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.item_commit, parent, false)
        val messageView: TextView = view.findViewById(R.id.tv_commit_message)
        val authorView: TextView = view.findViewById(R.id.tv_author_name)
        val dateView: TextView = view.findViewById(R.id.tv_commit_date)
        val avatarView: ImageView = view.findViewById(R.id.iv_author_avatar)

        val commit = getItem(position)
        messageView.text = commit.commitDetails.message
        authorView.text = commit.commitDetails.author.name
        
        // Formatação da data
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(commit.commitDetails.author.date)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateView.text = date?.let { outputFormat.format(it) } ?: commit.commitDetails.author.date
        } catch (e: Exception) {
            dateView.text = commit.commitDetails.author.date
        }

        // Carregamento de imagem corrigido
        val avatarUrl = commit.author?.avatarUrl
        avatarView.tag = avatarUrl
        
        if (avatarUrl != null) {
            val cached = avatarCache[avatarUrl]
            if (cached != null) {
                avatarView.setImageBitmap(cached)
            } else {
                avatarView.setImageResource(R.drawable.ic_person)
                executor.execute {
                    try {
                        val conn = URL(avatarUrl).openConnection()
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        val bitmap = BitmapFactory.decodeStream(conn.getInputStream())
                        if (bitmap != null) {
                            avatarCache[avatarUrl] = bitmap
                            handler.post {
                                if (avatarView.tag == avatarUrl) {
                                    avatarView.setImageBitmap(bitmap)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            avatarView.setImageResource(R.drawable.ic_person)
        }

        return view
    }
}
