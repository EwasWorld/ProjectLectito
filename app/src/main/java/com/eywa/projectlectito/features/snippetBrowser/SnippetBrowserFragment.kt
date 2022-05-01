package com.eywa.projectlectito.features.snippetBrowser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.eywa.projectlectito.R
import com.eywa.projectlectito.features.addSnippet.AddSnippetFragment
import com.eywa.projectlectito.utils.asVisibility
import kotlinx.android.synthetic.main.snippet_browser_fragment.*

class SnippetBrowserFragment : Fragment() {
    companion object {
        fun navigateTo(navController: NavController, textId: Int) {
            val bundle = Bundle()
            bundle.putInt("textId", textId)
            navController.navigate(R.id.snippetBrowserFragment, bundle)
        }
    }

    private val args: SnippetBrowserFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.snippet_browser_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.snippet_browser__title)

        val viewModel = ViewModelProvider(this)[SnippetBrowserViewModel::class.java]
        viewModel.textId.postValue(args.textId)

        viewModel.textName.observe(viewLifecycleOwner, { textName ->
            activity?.title = if (!textName.isNullOrBlank()) {
                textName
            }
            else {
                resources.getString(R.string.snippet_browser__title)
            }
        })

        val adapter = SnippetBrowserAdapter(viewModel, requireContext())
        recycler_sb.adapter = adapter
        recycler_sb.layoutManager = LinearLayoutManager(context)
        val itemDivider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDivider.setDrawable(
                ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.banner_vertical_divider,
                        requireContext().theme
                )!!
        )
        recycler_sb.addItemDecoration(itemDivider)
        viewModel.snippetsForText.observe(viewLifecycleOwner, { snippets ->
            text_sb__no_content.visibility = (snippets.isNullOrEmpty()).asVisibility()
            (snippets ?: listOf()).let {
                adapter.submitList(it)
                adapter.notifyDataSetChanged()
            }
        })

        button_sb__add_snippet.setOnClickListener {
            AddSnippetFragment.navigateTo(
                    findNavController(),
                    args.textId
            )
        }
    }
}