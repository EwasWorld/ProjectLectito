package com.eywa.projectlectito

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class WordDefinitionDetailView : ConstraintLayout {
    private lateinit var definitionTextView: TextView
    private lateinit var tagsTextView: TextView
    private lateinit var partsOfSpeechTextView: TextView
    private lateinit var indexTextView: TextView

    constructor(context: Context) : super(context) {
        initialise(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialise(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise(context, attrs)
    }

    private fun initialise(context: Context, attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.WordDefinitionDetailView)
        val definition = styledAttributes.getString(R.styleable.WordDefinitionDetailView_definition) ?: ""
        val partsOfSpeech = styledAttributes.getString(R.styleable.WordDefinitionDetailView_parts_of_speech) ?: ""
        val tags = styledAttributes.getString(R.styleable.WordDefinitionDetailView_tags) ?: ""
        val index = styledAttributes.getInt(R.styleable.WordDefinitionDetailView_index, 1)
        styledAttributes.recycle()

        val layout = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.word_definition_detail, this, true) as ConstraintLayout
        definitionTextView = layout.findViewById(R.id.text_definition_detail__definition)
        tagsTextView = layout.findViewById(R.id.text_definition_detail__tags)
        partsOfSpeechTextView = layout.findViewById(R.id.text_definition_detail__parts_of_speech)
        indexTextView = layout.findViewById(R.id.text_definition_detail__index)

        if (definition.isNotBlank()) {
            updateDefinition(definition)
        }
        if (partsOfSpeech.isNotBlank()) {
            updatePartsOfSpeech(partsOfSpeech)
        }
        updateTags(tags)
        updateIndex(index)
    }

    fun updateDefinition(value: String) {
        require(value.isNotBlank()) {"Definition cannot be blank"}
        definitionTextView.text = value
        invalidate()
        requestLayout()
    }

    fun updateTags(value: String) {
        if (value.isBlank()) {
            tagsTextView.visibility = View.GONE
        }
        else {
            tagsTextView.visibility = View.VISIBLE
            tagsTextView.text = value
        }
        invalidate()
        requestLayout()
    }

    fun updatePartsOfSpeech(value: String) {
        require(value.isNotBlank()) {"Parts of speech cannot be blank"}
        partsOfSpeechTextView.text = value
        invalidate()
        requestLayout()
    }

    fun updateIndex(value: Int) {
        require(value > 0) { "Index cannot be less than 1" }
        indexTextView.text = value.toString()
        invalidate()
        requestLayout()
    }
}