package com.eywa.projectlectito.addSnippet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import kotlinx.android.synthetic.main.add_snippet_fragment.*

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_snippet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.add_snippet__title)

        viewModel = ViewModelProvider(this)[AddSnippetViewModel::class.java]
        viewModel.textId.postValue(args.textId)

        viewModel.textName.observe(viewLifecycleOwner, {
            text_add_snippet__text_name.text = it ?: resources.getString(R.string.add_snippet__no_title)
        })
        viewModel.existingPagesOrdinals.observe(viewLifecycleOwner, {
            val ordinals = it
            Log.i("tag", ordinals.joinToString(","))
        })
        viewModel.pageExists.observe(viewLifecycleOwner, {
            text_add_snippet__duplicate_page_warning.visibility = it.asVisibility(true)
        })

        text_add_snippet__page.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                val newValue = p0.asInt()
                viewModel.pageReference.postValue(newValue)
            }
        })

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

    private fun submit(): Boolean {
        val missingFields = mutableListOf<Int>()

        val content = text_add_snippet__snippet_content.text
        if (content.isNullOrBlank()) {
            missingFields.add(R.string.add_snippet__missing_field_content)
        }

        val pageReference = text_add_snippet__page.text.asInt()
        if (pageReference == null) {
            missingFields.add(R.string.add_snippet__missing_field_page)
        }

        text_add_snippet__missing_field_warning.visibility = missingFields.isNotEmpty().asVisibility(true)
        if (missingFields.isNotEmpty()) {
            text_add_snippet__missing_field_warning.text =
                    resources.getString(R.string.add_snippet__required_fields_warning).format(
                            missingFields.joinToString(", ") { resources.getString(it) }
                    )
            return false
        }

        viewModel.insert(content.toString(), pageReference!!, text_add_snippet__chapter.text.asInt())
        return true
    }

    private fun Editable?.asInt() = this.toString().asInt()

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