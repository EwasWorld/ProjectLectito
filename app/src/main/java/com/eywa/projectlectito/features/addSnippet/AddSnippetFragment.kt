package com.eywa.projectlectito.features.addSnippet

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
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.utils.TextChangedListener
import com.eywa.projectlectito.utils.ToastSpamPrevention
import com.eywa.projectlectito.utils.asVisibility
import kotlinx.android.synthetic.main.add_snippet_fragment.*
import kotlinx.android.synthetic.main.edit_snippet_fragment.*

class AddSnippetFragment : Fragment() {
    companion object {
        fun navigateTo(navController: NavController, textId: Int) {
            val bundle = Bundle()
            bundle.putInt("textId", textId)
            navController.navigate(R.id.addSnippetFragment, bundle)
        }
    }

    private val args: AddSnippetFragmentArgs by navArgs()
    private lateinit var viewModel: AddSnippetViewModel
    private var pageExists = false
    private var contentHasErrors = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_snippet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.add_snippet__title)

        viewModel = ViewModelProvider(this)[AddSnippetViewModel::class.java]
        viewModel.textId.postValue(args.textId)

        viewModel.textName.observe(viewLifecycleOwner, {
            text_add_snippet__text_name.text = it ?: resources.getString(R.string.text_unknown_title)
        })
        viewModel.pageExists.observe(viewLifecycleOwner, {
            pageExists = it
            if (layout_add_snippet__page.userHasTouched) {
                layout_add_snippet__page.validate()
            }
        })

        setupValidation()

        button_add_snippet__submit.setOnClickListener {
            if (!submit()) {
                return@setOnClickListener
            }
            requireView().findNavController().popBackStack()
        }

        button_add_snippet__submit_and_another.setOnClickListener {
            submit()
        }
    }

    private fun setupValidation() {
        layout_add_snippet__page.textChangedListener = TextChangedListener {
            val newValue = it?.toString()?.asInt()
            viewModel.pageReference.postValue(newValue)
        }
        layout_add_snippet__page.validator = object : AddSnippetDetail.Validator {
            override fun getErrorString(content: String?): AddSnippetDetail.Validator.Errors? {
                if (content.isNullOrBlank()) {
                    return AddSnippetDetail.Validator.Errors(R.string.err__required_field)
                }
                if (content.asInt() == null) {
                    return AddSnippetDetail.Validator.Errors(R.string.err_add_snippet__not_int)
                }
                if (pageExists) {
                    return AddSnippetDetail.Validator.Errors(
                            R.string.err_add_snippet__duplicate_page_warning,
                            AddSnippetDetail.Validator.ErrorLevel.WARNING
                    )
                }
                return null
            }
        }
        layout_add_snippet__chapter.validator = object : AddSnippetDetail.Validator {
            override fun getErrorString(content: String?): AddSnippetDetail.Validator.Errors? {
                if (content.isNullOrBlank()) {
                    return AddSnippetDetail.Validator.Errors(R.string.err__required_field)
                }
                if (content.asInt() == null) {
                    return AddSnippetDetail.Validator.Errors(R.string.err_add_snippet__not_int)
                }
                return null
            }
        }
        text_add_snippet__snippet_content.addTextChangedListener(TextChangedListener { content ->
            validateSnippetContent(content?.toString())
        })
    }

    private fun validateSnippetContent(content: String?) {
        val errors = TextSnippet.isValidContent(content).getOrNull(0)
        contentHasErrors = errors != null
        text_add_snippet__content_warning.text = errors?.let { resources.getString(errors) } ?: ""
        text_add_snippet__content_warning.visibility = (errors != null).asVisibility()
    }

    private fun submit(): Boolean {
        validateSnippetContent(text_add_snippet__snippet_content.text?.toString())
        var hasErrors = contentHasErrors
        listOf(layout_add_snippet__page, layout_add_snippet__chapter).forEach {
            it.validate()
            hasErrors = hasErrors || it.hasErrors
        }

        if (hasErrors) {
            ToastSpamPrevention.displayToast(requireContext(), resources.getString(R.string.err_add_snippet__has_error))
            return false
        }

        val pageReference = layout_add_snippet__page.getValue().asInt()!!
        viewModel.insert(
                text_add_snippet__snippet_content.text.toString(),
                pageReference,
                layout_add_snippet__chapter.getValue().asInt()!!
        )

        text_add_snippet__snippet_content.setText("")
        layout_add_snippet__page.setValue((pageReference + 1).toString())
        viewModel.pageReference.postValue(pageReference + 1)
        return true
    }

    private fun String?.asInt(): Int? {
        if (this.isNullOrBlank()) return null
        try {
            return Integer.parseInt(this)
        }
        catch (e: Exception) {
            return null
        }
    }
}