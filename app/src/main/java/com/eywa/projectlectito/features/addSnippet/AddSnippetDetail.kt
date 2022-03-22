package com.eywa.projectlectito.features.addSnippet

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.eywa.projectlectito.R
import com.eywa.projectlectito.utils.androidWrappers.TextChangedListener
import com.eywa.projectlectito.utils.asVisibility
import com.eywa.projectlectito.utils.getColor

class AddSnippetDetail : ConstraintLayout {
    private lateinit var valueEditText: EditText
    private lateinit var errorTextView: TextView
    var validator: Validator? = null
    var textChangedListener: TextChangedListener? = null
    var hasErrors = false
        private set
    private var textHasChanged = false
    var userHasTouched = false

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
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.AddSnippetDetail)
        val label = styledAttributes.getString(R.styleable.AddSnippetDetail_label) ?: ""
        val hint = styledAttributes.getString(R.styleable.AddSnippetDetail_hint) ?: ""
        styledAttributes.recycle()

        val layout = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.add_snippet_detail, this, true) as ConstraintLayout

        layout.findViewById<TextView>(R.id.text_asd__label).text = label
        valueEditText = layout.findViewById(R.id.input_asd__value)
        valueEditText.hint = hint
        errorTextView = layout.findViewById(R.id.text_asd__error)

        valueEditText.addTextChangedListener(TextChangedListener { newText ->
            userHasTouched = true
            textHasChanged = true
            textChangedListener?.onTextChanged?.invoke(newText)
            validate(newText?.toString())
        })
        // Validate if the user clicks on the box then clicks away
        valueEditText.setOnFocusChangeListener { _, hasFocus ->
            userHasTouched = true
            if (hasFocus || textHasChanged) return@setOnFocusChangeListener
            validate(null)
        }
    }

    fun validate() {
        validate(valueEditText.text?.toString())
    }

    private fun validate(content: String?) {
        validator?.let {
            val errors = it.getErrorString(content)
            hasErrors = errors != null

            errorTextView.visibility = (errors != null).asVisibility()
            valueEditText.setTextColor(
                    context.theme.getColor(
                            errors?.level.let { level ->
                                if (level == Validator.ErrorLevel.ERROR) level.color else R.attr.general_text
                            }
                    )
            )
            errorTextView.setTextColor(context.theme.getColor((errors?.level ?: Validator.ErrorLevel.ERROR).color))
            errorTextView.text = errors?.getMessage(resources)
        }
    }

    fun getValue() = valueEditText.text?.toString()
    fun setValue(value: String) = valueEditText.setText(value)

    interface Validator {
        /**
         * @return null if [content] is valid. Error string if [content] is invalid
         */
        fun getErrorString(content: String?): Errors?

        data class Errors(private val message: String?, @StringRes private val messageId: Int?, val level: ErrorLevel) {
            constructor(message: String, level: ErrorLevel = ErrorLevel.ERROR) : this(message, null, level)
            constructor(@StringRes messageId: Int, level: ErrorLevel = ErrorLevel.ERROR) : this(null, messageId, level)

            fun getMessage(resources: Resources): String {
                if (message != null) return message
                if (messageId != null) return resources.getString(messageId)
                throw IllegalStateException("No message")
            }
        }

        enum class ErrorLevel(@AttrRes val color: Int) {
            ERROR(R.attr.warning_red),
            WARNING(R.attr.warning_orange)
        }
    }
}
