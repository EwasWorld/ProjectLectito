package com.eywa.projectlectito.features.viewTexts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.eywa.projectlectito.R
import kotlinx.android.synthetic.main.view_texts_fragment.*

class ViewTextsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_texts_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = resources.getString(R.string.view_texts__title)

        val adapter = ViewTextsAdapter()
        recycler_vt__texts.adapter = adapter
        recycler_vt__texts.layoutManager = LinearLayoutManager(context)

        val viewModel = ViewModelProvider(this)[ViewTextsViewModel::class.java]
        viewModel.allTexts.observe(viewLifecycleOwner, { texts ->
            texts?.let { adapter.submitList(it) }
        })

        button_vt__add_text.setOnClickListener {
            AddTextDialog().show(childFragmentManager, "dialog_vt__add_text")
        }
    }
}