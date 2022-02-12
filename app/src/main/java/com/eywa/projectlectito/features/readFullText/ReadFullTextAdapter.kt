package com.eywa.projectlectito.features.readFullText

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.snippets.TextSnippet

class ReadFullTextAdapter :
        ListAdapter<ReadFullTextAdapter.TextSnippetWithChar, ReadFullTextAdapter.RftSnippetViewHolder>(
                object : DiffUtil.ItemCallback<TextSnippetWithChar>() {
                    override fun areItemsTheSame(oldItem: TextSnippetWithChar, newItem: TextSnippetWithChar): Boolean {
                        return oldItem.snippet.id == newItem.snippet.id
                    }

                    override fun areContentsTheSame(
                            oldItem: TextSnippetWithChar,
                            newItem: TextSnippetWithChar
                    ): Boolean {
                        return newItem.snippet == oldItem.snippet
                    }
                }
        ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RftSnippetViewHolder {
        return RftSnippetViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.read_full_text_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RftSnippetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RftSnippetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: TextSnippetWithChar) {
            itemView.findViewById<TextView>(R.id.text_rfti__snippet_info).text = item.snippet.getChapterPageString()

            val snippetLength = item.snippet.content.length
            val spannableString = SpannableString(item.snippet.content)
            if (item.currentCharacter != null && item.currentCharacter >= 0 && item.currentCharacter < snippetLength) {
                spannableString.setSpan(
                        ForegroundColorSpan(Color.RED),
                        item.currentCharacter,
                        (item.currentCharacter + 3).coerceAtMost(snippetLength - 1),
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            itemView.findViewById<TextView>(R.id.text_rfti__content).text = spannableString
        }
    }

    data class TextSnippetWithChar(
            val snippet: TextSnippet,
            val currentCharacter: Int?
    )
}