package com.eywa.projectlectito.viewTexts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.ToastSpamPrevention
import com.eywa.projectlectito.addSnippet.AddSnippetFragment
import com.eywa.projectlectito.asVisibility
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.readSentence.ReadSentenceFragment
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

            itemView.setOnClickListener {
                if (item.totalSnippets == 0) {
                    ToastSpamPrevention.displayToast(
                            itemView.context,
                            itemView.resources.getString(R.string.view_texts__no_content_error)
                    )
                    return@setOnClickListener
                }
                ReadSentenceFragment.navigateTo(
                        itemView.findNavController(),
                        item.text.id,
                        item.text.currentSnippetId,
                        item.text.currentCharacterIndex
                )
            }
            itemView.setOnLongClickListener {
                AddSnippetFragment.navigateTo(itemView.findNavController(), item.text.id)
                return@setOnLongClickListener true
            }

            val progressView = itemView.findViewById<TextView>(R.id.text_view_texts_item__progress)
            val currentView = itemView.findViewById<TextView>(R.id.text_view_texts_item__current)
            val delimView = itemView.findViewById<TextView>(R.id.text_view_texts_item__jp_delim)

            // No snippets
            if (item.totalSnippets == 0) {
                currentView.text = itemView.resources.getString(R.string.view_texts__no_content)
                progressView.visibility = false.asVisibility()
                delimView.visibility = false.asVisibility()
                return
            }

            val isTextStarted = item.percentageRead != 0.0
            progressView.visibility = isTextStarted.asVisibility()
            delimView.visibility = isTextStarted.asVisibility()

            if (isTextStarted) {
                @SuppressLint("SetTextI18n") // Literal only used for a symbol
                progressView.text = (item.percentageRead * 100).roundToInt().toString() + "%"
            }
            currentView.text = item.currentSnippet?.getChapterPageString()
                    ?: itemView.resources.getString(R.string.view_texts__not_started)
        }
    }
}