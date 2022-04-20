package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.eywa.projectlectito.features.readSentence.DotIndicatorInfo

/**
 * Horizontal Linear Layout of small dots to indicate the total number of items and what item is currently selected
 */
class DotIndicatorLayoutView : LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setView(totalItems: Int, selectedIndex: Int) {
        removeAllViews()
        val dotInfo = DotIndicatorInfo.getDotsList(totalItems, selectedIndex)
        for (dot in dotInfo) {
            addView(DotIndicatorImageView(context).apply {
                isCurrentDot = dot.isSelected
                isMini = dot.isMini
            })
        }

        invalidate()
        requestLayout()
    }
}