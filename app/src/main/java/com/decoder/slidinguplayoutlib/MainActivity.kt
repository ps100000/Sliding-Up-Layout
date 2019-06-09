package com.decoder.slidinguplayoutlib


import com.decoder.slidinguplayout.SlidingUpLayout
import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<RelativeLayout>(R.id.aaa).findViewById<Button>(R.id.button).setOnClickListener{
            findViewById<SlidingUpLayout>(R.id.parent).setState(SlidingUpLayout.State.UP)
        }

        findViewById<SlidingUpLayout>(R.id.parent).setOnMoveListener( object : SlidingUpLayout.OnDragListener{
            override fun onDrag(pos: Float) {
                findViewById<TextView>(R.id.textViewdelta).text = pos.toString()
            }

        })

        findViewById<SlidingUpLayout>(R.id.parent).setOnStateChangeListener( object : SlidingUpLayout.OnStateChangeListener{
            override fun onChange(state: SlidingUpLayout.State) {
                findViewById<TextView>(R.id.textViewstate).text = state.toString()
            }

        })

    }
}