package com.eywa.projectlectito.editSnippet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.eywa.projectlectito.R
import com.eywa.projectlectito.asVisibility
import kotlinx.android.synthetic.main.edit_snippet_fragment.*

class EditSnippetFragment : Fragment() {
    companion object {
        fun navigateTo(navController: NavController, snippetId: Int, startCharacter: Int, endCharacterExclusive: Int?) {
            val bundle = Bundle()
            bundle.putInt("snippetId", snippetId)
            bundle.putInt("startCharacter", startCharacter)
            bundle.putInt("endCharacterExclusive", endCharacterExclusive ?: -1)
            navController.navigate(R.id.editSnippetFragment, bundle)
        }
    }

    private val args: EditSnippetFragmentArgs by navArgs()
    private lateinit var viewModel: EditSnippetViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.edit_snippet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.edit_snippet__title)

        viewModel = ViewModelProvider(this)[EditSnippetViewModel::class.java]

        postInitialValues()
        retrieveValuesFromVm()
        setupButtonListeners()
    }

    private fun postInitialValues() {
        viewModel.snippetId.postValue(args.snippetId)
        val end = args.endCharacterExclusive.let { end -> if (end == -1) null else end }
        viewModel.startEnd.postValue(args.startCharacter to end)
    }

    private fun retrieveValuesFromVm() {
        viewModel.textName.observe(viewLifecycleOwner, {
            text_edit_snippet__title.text = it ?: resources.getString(R.string.text_unknown_title)
        })
        viewModel.pageInfo.observe(viewLifecycleOwner, {
            it?.let { pageInfo -> text_edit_snippet__page_ref.text = pageInfo }
            text_edit_snippet__page_ref.visibility = (it != null).asVisibility()
        })
        viewModel.editSection.observe(viewLifecycleOwner, {
            // TODO POLISH Show an error if no text
            input_text_edit_snippet__content.setText(it ?: "")
        })
    }

    private fun setupButtonListeners() {
        button_edit_snippet__cancel.setOnClickListener {
            requireView().findNavController().popBackStack()
        }

        button_edit_snippet__complete.setOnClickListener {
            val newContent = input_text_edit_snippet__content.text.toString()
            // TODO Warn the user if content is blank
            viewModel.update(newContent)
            requireView().findNavController().popBackStack()
        }
    }
}