package com.eywa.projectlectito.viewTexts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.asVisibility
import com.eywa.projectlectito.database.texts.Text
import kotlin.math.roundToInt

class ViewTextsAdapter : ListAdapter<Text.WithCurrentSnippetInfo, ViewTextsAdapter.TextViewHolder>(
        object : DiffUtil.ItemCallback<Text.WithCurrentSnippetInfo>() {
            override fun areItemsTheSame(
                    oldItem: Text.WithCurrentSnippetInfo,
                    newItem: Text.WithCurrentSnippetInfo
            ): Boolean {
                return oldItem.text.id == newItem.text.id
            }

            override fun areContentsTheSame(
                    oldItem: Text.WithCurrentSnippetInfo,
                    newItem: Text.WithCurrentSnippetInfo
            ): Boolean {
                return newItem == oldItem
            }
        }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.view_texts_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Text.WithCurrentSnippetInfo) {
            itemView.findViewById<TextView>(R.id.text_view_texts_item__title).text = item.text.name

            val progressView = itemView.findViewById<TextView>(R.id.text_view_texts_item__progress)
            val isTextStarted = item.percentageRead != 0.0
            progressView.visibility = isTextStarted.asVisibility()
            itemView.findViewById<TextView>(R.id.text_view_texts_item__jp_delim).visibility =
                    isTextStarted.asVisibility()
            if (isTextStarted) {
                @SuppressLint("SetTextI18n") // Literal only used for a symbol
                progressView.text = (item.percentageRead * 100).roundToInt().toString() + "%"
            }

            itemView.findViewById<TextView>(R.id.text_view_texts_item__current).text =
                    item.currentSnippet?.getChapterPageString()
                            ?: itemView.resources.getString(R.string.view_texts__not_started)
        }
    }
}