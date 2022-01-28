package com.eywa.projectlectito.viewTexts

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
        activity?.title = resources.getString(R.string.read_sentence__title)

        val adapter = ViewTextsAdapter()
        recycler_view_texts.adapter = adapter
        recycler_view_texts.layoutManager = LinearLayoutManager(context)

        val viewModel = ViewModelProvider(this)[ViewTextsViewModel::class.java]
        viewModel.allTexts.observe(viewLifecycleOwner, { texts ->
            texts?.let { adapter.submitList(it) }
        })
    }
}