package com.eywa.projectlectito.features.snippetBrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.features.readSentence.ReadSentenceFragment

class SnippetBrowserAdapter :
        ListAdapter<TextSnippet, SnippetBrowserAdapter.SbSnippetViewHolder>(
                object : DiffUtil.ItemCallback<TextSnippet>() {
                    override fun areItemsTheSame(oldItem: TextSnippet, newItem: TextSnippet): Boolean {
                        return oldItem.id == newItem.id
                    }

                    override fun areContentsTheSame(oldItem: TextSnippet, newItem: TextSnippet): Boolean {
                        return newItem == oldItem
                    }
                }
        ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SbSnippetViewHolder {
        return SbSnippetViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.snippet_browser_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SbSnippetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SbSnippetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: TextSnippet) {
            itemView.findViewById<TextView>(R.id.text_sbi__chapter_page).text = item.getChapterPageString()

            itemView.setOnClickListener {
                ReadSentenceFragment.navigateTo(it.findNavController(), item.textId, item.id, 0)
            }
        }
    }
}