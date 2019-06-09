package com.decoder.slidinguplayout

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.lang.Exception






class SlidingUpLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var offsetY = 0
    private var barHeight = 0
    private var minHeight = 0
    private var maxHeight = 0
    private var moving = false
    private var movingUp = true
    private var top: View? = null

    private var changelistener: OnStateChangeListener? = null
    private var draglistener: OnDragListener? = null

    private var overlapBar: Boolean = false
    private val animationUpdater: ValueAnimator.AnimatorUpdateListener = ValueAnimator.AnimatorUpdateListener{
        val value = it.animatedValue as Int
        top!!.layoutParams.height = value
        top!!.requestLayout()
        if ((it.animatedValue as Int) == 0) {
            top!!.visibility = FrameLayout.GONE
        }
        draglistener?.onDrag((value - minHeight) / (maxHeight - minHeight).toFloat())
    }

    interface OnStateChangeListener {
        fun onChange(state: State)
    }

    interface OnDragListener {
        fun onDrag(pos: Float)
    }

    init {
        barHeight = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
        for (i in 0 until attrs.attributeCount) {
            when (attrs.getAttributeName(i)) {
                "barHeight" ->{
                    var v = attrs.getAttributeValue(i)
                    if (v.endsWith("dip")){
                        v = v.substring(0, v.lastIndexOf("dip"))
                        barHeight = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
                    }else{
                        throw Exception("barHeight not in dip")
                    }
                }
                "overlapBar" ->
                    overlapBar = attrs.getAttributeBooleanValue(i, false)
            }
        }
        super.setOrientation(VERTICAL)
    }

    enum class State(state: Int){

        UP(1),
        DOWN(2);

        override fun toString(): String {
            return  if (this == UP){
                "UP"
            }else{
                "DOWN"
            }
        }

    }
    override fun setOrientation(orientation: Int) {
        super.setOrientation(VERTICAL)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (getChildAt(1).layoutParams.height > 0){
            minHeight = height - getChildAt(1).layoutParams.height
        }else{
            minHeight = 0
        }
        if(overlapBar){
            minHeight += barHeight
        }

    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if ((childCount != 2)) {
            throw Exception("invalid number of children")
        }else{
            top = getChildAt(0)
            if(top !is FrameLayout){
                throw Exception("first child needs to be a FrameLayout surrounding your toplayout")
            }
            if ((top as FrameLayout).childCount > 1){
                throw Exception("first child should only have one child: The toplayout")
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if ((maxHeight == 0) and (measuredHeight > 0)) {
            if (overlapBar){
                maxHeight = measuredHeight
                val mp = getChildAt(1).layoutParams as MarginLayoutParams
                mp.topMargin = -barHeight
                getChildAt(1).layoutParams = mp
                getChildAt(1).requestLayout()
            }else {
                maxHeight = measuredHeight - barHeight
            }
            if(top != null) {
                top?.layoutParams?.height = maxHeight
                top?.requestLayout()
                (top as FrameLayout).getChildAt(0).layoutParams.height = maxHeight
                (top as FrameLayout).getChildAt(0).requestLayout()
            }
        }

    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return false
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return false
    }

    override fun onViewAdded(child: View?) {
        if (childCount <= 2) {
            super.onViewAdded(child)
        } else {
            throw Exception("invalid number of children")
        }
    }

    override fun onViewRemoved(child: View?) {
        throw Exception("invalid number of children")
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && top != null) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    offsetY = event.y.toInt()
                    if ((offsetY - getChildAt(1).top <= barHeight) && (offsetY - getChildAt(1).top >= 0)) {
                        moving = true
                        if(!movingUp) {
                            top!!.visibility = FrameLayout.VISIBLE
                        }
                        top!!.requestLayout()
                    }

                }
                MotionEvent.ACTION_MOVE -> {
                    if(moving) {
                        var y = event.y.toInt() - offsetY

                        //Log.d("pos", String.format("raw y: %d", y))
                        if (movingUp) {
                            y += maxHeight
                        }else{
                            y += minHeight
                        }
                        if (y > maxHeight) {
                            y = maxHeight
                        }
                        if (y < minHeight) {
                            y = minHeight
                        }
                        top!!.layoutParams.height = y

                        top!!.requestLayout()
                        //Log.d("pos", String.format("y: %d", y))

                        draglistener?.onDrag((y - minHeight) / (maxHeight - minHeight).toFloat())
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if(moving) {
                        val startHeight = top!!.height
                        val valueAnimator: ValueAnimator?

                        if (startHeight - minHeight < if (movingUp) {
                                2
                            } else {
                                1
                            } * (maxHeight - minHeight) / 3
                        ) {
                            movingUp = false
                            valueAnimator = ValueAnimator.ofInt(startHeight, minHeight)
                            valueAnimator.duration = ((startHeight / (maxHeight - minHeight).toFloat()) * 250).toLong()
                            changelistener?.onChange(State.UP)
                        } else {
                            movingUp = true
                            valueAnimator = ValueAnimator.ofInt(startHeight, maxHeight)
                            valueAnimator.duration = (((maxHeight - startHeight) / (maxHeight - minHeight).toFloat()) * 250).toLong()
                            changelistener?.onChange(State.DOWN)
                        }
                        moving = false
                        valueAnimator.addUpdateListener(animationUpdater)
                        valueAnimator.interpolator = AccelerateInterpolator(1.5f)
                        valueAnimator.start()
                    }
                }
                else -> {
                }
            }
        }
        if (!moving) {
            super.dispatchTouchEvent(event)
        }
        return true
    }


    fun setState(state: State){
        changelistener?.onChange(state)
        val valueAnimator: ValueAnimator
        when(state){
            State.UP ->{
                movingUp = false
                valueAnimator = ValueAnimator.ofInt(top!!.height, minHeight)
                valueAnimator.duration = ((top!!.height / (maxHeight - minHeight).toFloat()) * 250).toLong()
            }
            State.DOWN -> {
                movingUp = true
                valueAnimator = ValueAnimator.ofInt(top!!.height, maxHeight)
                valueAnimator.duration = (((maxHeight - top!!.height) / (maxHeight - minHeight).toFloat()) * 250).toLong()
            }
        }

        valueAnimator.addUpdateListener(animationUpdater)
        valueAnimator.interpolator = AccelerateInterpolator(1.5f)
        valueAnimator.start()
    }

    fun getState(): State {
        return if(movingUp){
            State.DOWN
        }else{
            State.UP
        }
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener){
        changelistener = listener
    }


    fun setOnMoveListener(listener: OnDragListener){
        draglistener = listener
    }

}
