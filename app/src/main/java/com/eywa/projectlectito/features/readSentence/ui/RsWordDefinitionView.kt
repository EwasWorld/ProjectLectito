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
import androidx.viewpager2.widget.ViewPager2
import com.eywa.projectlectito.R
import com.eywa.projectlectito.databinding.RsWordDefinitionBinding
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceIntent
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceMviViewModel
import com.eywa.projectlectito.features.readSentence.mvi.ReadSentenceViewState.WordDefinitionState
import com.eywa.projectlectito.features.readSentence.wordDefinitions.WordDefinitionPageView
import kotlinx.android.synthetic.main.rs_word_definition.view.*


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
        viewModel.wordDefinitionPagerInfo.observe(layout.lifecycleOwner!!, {
            it?.let { hasWord ->
                pager_read_sentence.adapter = ScreenSlidePagerAdapter(FragmentManager.findFragment(this), hasWord)
            }
        })
        viewModel.viewState.observe(layout.lifecycleOwner!!, {
            text_read_sentence__general_message.text = it.wordDefinitionState.getMessage(resources)
            it.wordDefinitionState.asHasWord()?.let { hasWord ->
                layout_read_sentence__definition_dots.setView(hasWord.size, hasWord.selectedIndex)
            }
            requestLayout()
        })

        button_read_sentence__close_definition.setOnClickListener {
            viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnClosePressed)
        }
        pager_read_sentence.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.handle(ReadSentenceIntent.WordDefinitionIntent.OnDefinitionPageChanged(position))
            }
        })
    }

    private inner class ScreenSlidePagerAdapter(
            activity: Fragment,
            private val data: WordDefinitionState.HasWord.ViewPagerInfo
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = data.size
        override fun createFragment(position: Int): Fragment = WordDefinitionPageView(data.getDataForIndex(position))
    }
}

