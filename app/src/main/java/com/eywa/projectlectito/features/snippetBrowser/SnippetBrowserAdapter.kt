package com.eywa.projectlectito.features.snippetBrowser

import android.app.AlertDialog
import android.view.*
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.features.readSentence.ReadSentenceFragment

class SnippetBrowserAdapter(private val viewModel: SnippetBrowserViewModel) :
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
                LayoutInflater.from(parent.context).inflate(R.layout.snippet_browser_item, parent, false),
                viewModel
        )
    }

    override fun onBindViewHolder(holder: SbSnippetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SbSnippetViewHolder(view: View, val viewModel: SnippetBrowserViewModel) : RecyclerView.ViewHolder(view),
            View.OnCreateContextMenuListener {
        lateinit var item: TextSnippet

        init {
            view.setOnCreateContextMenuListener(this)
        }

        fun bind(item: TextSnippet) {
            this.item = item
            itemView.findViewById<TextView>(R.id.text_sbi__chapter_page).text = item.getChapterPageString()

            itemView.setOnClickListener {
                ReadSentenceFragment.navigateTo(it.findNavController(), item.textId, item.id, 0)
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            ContextMenuItem.values().forEach {
                it.addItemToMenu(menu, itemView, item, viewModel)
            }
        }

        enum class ContextMenuItem(private val titleId: Int) {
            READ_SENTENCE(R.string.view_texts__read_sentence) {
                override fun onClick(view: View, item: TextSnippet, viewModel: SnippetBrowserViewModel) {
                    ReadSentenceFragment.navigateTo(view.findNavController(), item.textId, item.id, 0)
                }
            },
            DELETE_TEXT(R.string.view_texts__delete_text) {
                private var confirmDeleteDialog: AlertDialog? = null

                override fun onClick(view: View, item: TextSnippet, viewModel: SnippetBrowserViewModel) {
                    if (confirmDeleteDialog == null) {
                        confirmDeleteDialog = AlertDialog.Builder(view.context)
                                .setTitle(R.string.view_texts__confirm_delete_dialog_title)
                                .setMessage(
                                        view.resources.getString(R.string.view_texts__confirm_delete_dialog_body)
                                                .format(item.getChapterPageString())
                                )
                                .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(item.id) }
                                .setNegativeButton(R.string.cancel) { _, _ -> }
                                .create()
                    }

                    confirmDeleteDialog!!.show()
                }
            };

            open fun addItemToMenu(
                    menu: ContextMenu,
                    view: View,
                    text: TextSnippet,
                    viewModel: SnippetBrowserViewModel
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

            abstract fun onClick(view: View, item: TextSnippet, viewModel: SnippetBrowserViewModel)
        }
    }
}