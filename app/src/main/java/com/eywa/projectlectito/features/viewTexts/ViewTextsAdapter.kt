package com.eywa.projectlectito.features.viewTexts

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.eywa.projectlectito.features.snippetBrowser.SnippetBrowserFragment
import com.eywa.projectlectito.utils.androidWrappers.ToastSpamPrevention
import com.eywa.projectlectito.utils.asVisibility
import kotlin.math.roundToInt

class ViewTextsAdapter(private val viewModel: ViewTextsViewModel) :
        ListAdapter<Text.WithCurrentSnippetInfo, ViewTextsAdapter.TextViewHolder>(
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
                LayoutInflater.from(parent.context).inflate(R.layout.view_texts_list_item, parent, false),
                viewModel
        )
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TextViewHolder(view: View, val viewModel: ViewTextsViewModel) : RecyclerView.ViewHolder(view),
            View.OnCreateContextMenuListener {
        lateinit var item: Text.WithCurrentSnippetInfo

        init {
            view.setOnCreateContextMenuListener(this)
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

            val isTextStarted = item.percentageRead != null
            progressView.visibility = isTextStarted.asVisibility()
            delimView.visibility = isTextStarted.asVisibility()

            if (isTextStarted) {
                @SuppressLint("SetTextI18n") // Literal only used for a symbol
                progressView.text = (item.percentageRead!! * 100).roundToInt().toString() + "%"
            }
            currentView.text = when {
                isTextStarted -> item.currentSnippet?.getChapterPageString()
                item.text.isComplete -> itemView.resources.getString(R.string.view_texts__complete)
                else -> itemView.resources.getString(R.string.view_texts__not_started)
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            ContextMenuItem.values().forEach {
                it.addItemToMenu(menu, itemView, item, viewModel)
            }
        }

        enum class ContextMenuItem(private val titleId: Int) {
            READ_SENTENCE(R.string.view_texts__read_sentence) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel) {
                    item.readSnippet(view)
                }
            },
            SNIPPET_BROWSER(R.string.snippet_browser__title) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel) {
                    if (item.checkForContentAndAlert(view)) {
                        SnippetBrowserFragment.navigateTo(view.findNavController(), item.text.id)
                    }
                }
            },
            READ_FULL(R.string.read_full_text__title) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel) {
                    if (item.checkForContentAndAlert(view)) {
                        ReadFullTextFragment.navigateTo(view.findNavController(), item.text.id)
                    }
                }
            },
            ADD_SNIPPET(R.string.add_snippet__title) {
                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel) {
                    AddSnippetFragment.navigateTo(view.findNavController(), item.text.id)
                }
            },
            DELETE_TEXT(R.string.view_texts__delete_text) {
                private var confirmDeleteDialog: AlertDialog? = null

                override fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel) {
                    if (confirmDeleteDialog == null) {
                        confirmDeleteDialog = AlertDialog.Builder(view.context)
                                .setTitle(R.string.view_texts__confirm_delete_dialog_title)
                                .setMessage(
                                        view.resources.getString(R.string.view_texts__confirm_delete_dialog_body)
                                                .format(item.text.name)
                                )
                                .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(item.text.id) }
                                .setNegativeButton(R.string.cancel) { _, _ -> }
                                .create()
                    }

                    confirmDeleteDialog!!.show()
                }
            };

            open fun addItemToMenu(
                    menu: ContextMenu,
                    view: View,
                    text: Text.WithCurrentSnippetInfo,
                    viewModel: ViewTextsViewModel
            ) {
                val newItem = menu.add(Menu.NONE, ordinal, ordinal, titleId)
                newItem.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                        if (item == null || item.itemId >= values().size) {
                            return false
                        }
                        values()[item.itemId].onClick(view, text, viewModel)
                        return true
                    }
                })
            }

            abstract fun onClick(view: View, item: Text.WithCurrentSnippetInfo, viewModel: ViewTextsViewModel)
        }

        companion object {
            /**
             * Checks the text has content. If it doesn't, displays a toast message
             * @return true if there is content
             */
            private fun Text.WithCurrentSnippetInfo.checkForContentAndAlert(view: View): Boolean {
                if (totalSnippets == 0) {
                    ToastSpamPrevention.displayToast(
                            view.context,
                            view.resources.getString(R.string.view_texts__no_content_error)
                    )
                    return false
                }
                return true
            }

            private fun Text.WithCurrentSnippetInfo.readSnippet(view: View) {
                if (checkForContentAndAlert(view)) {
                    ReadSentenceFragment.navigateTo(
                            view.findNavController(),
                            text.id,
                            text.currentSnippetId,
                            text.currentCharacterIndex
                    )
                }
            }
        }
    }
}