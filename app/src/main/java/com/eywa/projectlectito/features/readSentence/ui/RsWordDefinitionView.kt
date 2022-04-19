package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsWordDefinitionBinding
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.*
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionPageView
import kotlinx.android.synthetic.main.rs_word_definition.view.*
import kotlinx.android.synthetic.main.rs_word_definition_page.view.*


class RsWordDefinitionView : ConstraintLayout {
    companion object {
        private const val LOG_TAG = "RsWordDefinitionView"
    }

    private lateinit var layout: RsWordDefinitionBinding
    private lateinit var viewModel: ReadSentenceMviViewModel

    constructor(context: Context) : super(context) {
        initialise(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialise(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise(context)
    }

    private fun initialise(context: Context) {
        if (isInEditMode) {
            LayoutInflater.from(context).inflate(R.layout.rs_word_definition, this, true)
        }
        else {
            layout = RsWordDefinitionBinding.inflate(LayoutInflater.from(context), this, true)
        }
    }

    fun setLifecycleInfo(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner) {
        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadSentenceMviViewModel::class.java]
        layout.lifecycleOwner = lifecycleOwner
        layout.viewModel = viewModel

        setupListeners()
    }

    private fun setupListeners() {
        viewModel.viewState.observe(layout.lifecycleOwner!!, {
            pager_read_sentence.requestLayout()
            it.wordDefinitionState.asHasWord().setPagerAdapter()
        })

        button_read_sentence__close_definition.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnClosePressed)
        }
    }

    private fun WordDefinitionState.HasWord?.setPagerAdapter() {
        this ?: return
        pager_read_sentence.adapter =
                ScreenSlidePagerAdapter(FragmentManager.findFragment(this@RsWordDefinitionView), this)
    }

    private inner class ScreenSlidePagerAdapter(
            activity: Fragment,
            private val data: WordDefinitionState.HasWord
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = data.getDefinitionCount()
        override fun createFragment(position: Int): Fragment = WordDefinitionPageView(data.getDataForIndex(position))
    }
}

