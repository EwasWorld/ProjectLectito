package com.eywa.projectlectito.features.viewTexts

import android.annotation.SuppressLint
import android.view.*
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.texts.Text
import com.eywa.projectlectito.features.addSnippet.AddSnippetFragment
import com.eywa.projectlectito.features.readFullText.ReadFullTextFragment
import com.eywa.projectlectito.features.readSentence.ReadSentenceFragment
import com.eywa.projectlectito.utils.ToastSpamPrevention
import com.eywa.projectlectito.utils.asVisibility
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

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        lateinit var item: Text.WithCurrentSnippetInfo

        init {
            view.setOnCreateContextMenuListener(this)
        }

        companion object {
            private fun Text.WithCurrentSnippetInfo.readSnippet(view: View) {
                if (totalSnippets == 0) {
                    ToastSpamPrevention.displayToast(
                            view.context,
                            view.resources.getString(R.string.view_texts__no_content_error)
                    )
                    return
                }
                ReadSentenceFragment.navigateTo(
                        view.findNavController(),
                        text.id,
                        text.currentSnippetId,
                        text.currentCharacterIndex
                )
            }
        }

        fun bind(item: Text.WithCurrentSnippetInfo) {
            this.item = item
            itemView.findViewById<TextView>(R.id.text_view_texts_item__title).text = item.text.name

            itemView.setOnClickListener { item.readSnippet(it) }

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

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            ContextMenuItem.values().forEach {
                it.addItemToMenu(menu, itemView, item)
            }
        }

        enum class ContextMenuItem(private val titleId: Int) {
            READ_SENTENCE(R.string.view_texts__read_sentence) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo) {
                    item.readSnippet(view)
                }
            },
            READ_FULL(R.string.read_full_text__title) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo) {
                    // TODO Check for content first
                    ReadFullTextFragment.navigateTo(view.findNavController(), item.text.id)
                }
            },
            ADD_SNIPPET(R.string.add_snippet__title) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo) {
                    AddSnippetFragment.navigateTo(view.findNavController(), item.text.id)
                }
            };

            open fun addItemToMenu(menu: ContextMenu, view: View, text: Text.WithCurrentSnippetInfo) {
                val newItem = menu.add(Menu.NONE, ordinal, ordinal, titleId)
                newItem.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                        if (item == null || item.itemId >= values().size) {
                            return false
                        }
                        values()[item.itemId].onClick(view, text)
                        return true
                    }
                })
            }

            abstract fun onClick(view: View, item: Text.WithCurrentSnippetInfo)
        }
    }
}