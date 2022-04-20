package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.eywa.projectlectito.R
import com.eywa.projectlectito.utils.getColor

/**
 * Creates a single dot to go on a [DotIndicatorLayoutView]
 */
class DotIndicatorImageView : androidx.appcompat.widget.AppCompatImageView {
    var isMini = false
        set(value) {
            if (value == field) return
            field = value

            val desiredSize = resources.getDimensionPixelSize(
                    if (isMini) R.dimen.mini_indicator_dot_size else R.dimen.indicator_dot_size
            )
            layoutParams = LinearLayout.LayoutParams(desiredSize, desiredSize)
        }

    var isCurrentDot = false
        set(value) {
            if (value == field) return
            field = value

            alpha = if (isCurrentDot) 1f else .4f
        }

    constructor(context: Context) : super(context) {
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise()
    }

    private fun initialise() {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.default_dot, context.theme)!!
        DrawableCompat.setTint(drawable, context.theme.getColor(R.attr.general_text))
        background = drawable

        alpha = if (isCurrentDot) 1f else .4f
        val desiredSize = resources.getDimensionPixelSize(
                if (isMini) R.dimen.mini_indicator_dot_size else R.dimen.indicator_dot_size
        )
        layoutParams = LinearLayout.LayoutParams(desiredSize, desiredSize)
    }
}