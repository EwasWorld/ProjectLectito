package com.eywa.projectlectito.features.readSentence.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import com.eywa.projectlectito.R
import kotlin.properties.Delegates


class HighlightOverlayView : View {
    private var viewToFrameId by Delegates.notNull<Int>()
    private var overlayColour by Delegates.notNull<Int>()
    private var framedContentRectangle: RectF? = null

    constructor(context: Context?) : super(context) {
        initialise(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialise(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise(attrs)
    }

    private fun initialise(attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.HighlightOverlayView)
        viewToFrameId = styledAttributes.getResourceIdOrThrow(R.styleable.HighlightOverlayView_view_to_frame_id)
        overlayColour = styledAttributes.getColorOrThrow(R.styleable.HighlightOverlayView_overlay_color)
        styledAttributes.recycle()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawBitmap(createWindowFrame(), 0f, 0f, null)
    }

    private fun createWindowFrame(): Bitmap {
        val viewToFrame = rootView.findViewById<View>(viewToFrameId)
        val viewToFrameLocation = IntArray(2)
        viewToFrame.getLocationInWindow(viewToFrameLocation)

        // The action bar is not taken into account by getLocationInWindow
        //    so find the absolute location of the top level layout in the fragment and use that to fix the frame
        val originOffset = IntArray(2)
        rootView.findViewById<View>(R.id.layout_read_sentence__top_level_container).getLocationInWindow(originOffset)

        val viewToFrameX = viewToFrameLocation[0].toFloat() - originOffset[0]
        val viewToFrameY = viewToFrameLocation[1].toFloat() - originOffset[1]

        val windowFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)!!
        val osCanvas = Canvas(windowFrame)
        val outerRectangle = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = overlayColour
        osCanvas.drawRect(outerRectangle, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        framedContentRectangle = RectF(
                viewToFrameX,
                viewToFrameY,
                viewToFrameX + viewToFrame.width,
                viewToFrameY + viewToFrame.height
        )
        osCanvas.drawRect(framedContentRectangle!!, paint)
        return windowFrame
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (framedContentRectangle?.contains(event?.x ?: -1f, event?.y ?: -1f) == true) {
            return false
        }
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}