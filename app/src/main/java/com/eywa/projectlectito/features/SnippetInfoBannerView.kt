package com.eywa.projectlectito.features

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.eywa.projectlectito.R
import com.eywa.projectlectito.database.chapter.TextChapter
import com.eywa.projectlectito.database.snippets.TextSnippet
import com.eywa.projectlectito.utils.JAPANESE_LIST_DELIMINATOR
import kotlinx.android.synthetic.main.snippet_info_banner.view.*

class SnippetInfoBannerView : ConstraintLayout {
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
        LayoutInflater.from(context).inflate(R.layout.snippet_info_banner, this, true)
    }

    fun setSnippet(snippet: TextSnippet, chapter: TextChapter?) {
        require(chapter == null || snippet.textId == chapter.textId) { "Invalid snippet and chapter" }

        val chapterName = chapter.takeIf { !it?.name.isNullOrBlank() }
                ?.let { "$JAPANESE_LIST_DELIMINATOR$it" } ?: ""
        text_sib__chapter.text = "%s%s".format(snippet.getChapterString(), chapterName)
        text_sib__page.text = snippet.getPageStringWithName()
    }
}