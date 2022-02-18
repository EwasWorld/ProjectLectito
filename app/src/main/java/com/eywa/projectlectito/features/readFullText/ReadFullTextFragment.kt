package com.eywa.projectlectito.features.readFullText

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eywa.projectlectito.R
import kotlinx.android.synthetic.main.read_full_text_fragment.*

class ReadFullTextFragment : Fragment() {
    companion object {
        fun navigateTo(
                navController: NavController,
                textId: Int,
                currentSnippetId: Int? = null,
                currentCharacter: Int? = null
        ) {
            val bundle = Bundle()
            bundle.putInt("textId", textId)
            bundle.putInt("currentSnippetId", currentSnippetId ?: -1)
            bundle.putInt("currentCharacter", currentCharacter ?: -1)
            navController.navigate(R.id.readFullTextFragment, bundle)
        }
    }

    private val args: ReadFullTextFragmentArgs by navArgs()
    private lateinit var viewModel: ReadFullTextViewModel

    private var displayedSnippetIds = listOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.read_full_text_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.read_full_text__title)

        viewModel = ViewModelProvider(this)[ReadFullTextViewModel::class.java]
        viewModel.textId.postValue(args.textId)

        viewModel.textName.observe(viewLifecycleOwner, { textName ->
            activity?.title = if (!textName.isNullOrBlank()) {
                textName
            }
            else {
                resources.getString(R.string.read_full_text__title)
            }
        })

        val adapter = ReadFullTextAdapter()
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int
            ) {
                if (args.currentSnippetId != -1) {
                    recycler_rft.scrollToPosition(displayedSnippetIds.indexOf(args.currentSnippetId))
                }
            }

            override fun onItemRangeRemoved(
                    positionStart: Int,
                    itemCount: Int
            ) {
                if (args.currentSnippetId != -1) {
                    recycler_rft.scrollToPosition(displayedSnippetIds.indexOf(args.currentSnippetId))
                }
                recycler_rft.smoothScrollToPosition(itemCount)
            }
        })

        recycler_rft.adapter = adapter
        recycler_rft.layoutManager = LinearLayoutManager(context)

        viewModel.snippetsForText.observe(viewLifecycleOwner, { snippets ->
            displayedSnippetIds = snippets?.map { it.id } ?: listOf()

            snippets?.let {
                adapter.submitList(it.map { snippet ->
                    ReadFullTextAdapter.TextSnippetWithChar(
                            snippet,
                            if (snippet.id == args.currentSnippetId) args.currentCharacter.coerceAtLeast(0) else null
                    )
                })
                adapter.notifyDataSetChanged()
            }
        })
    }
}