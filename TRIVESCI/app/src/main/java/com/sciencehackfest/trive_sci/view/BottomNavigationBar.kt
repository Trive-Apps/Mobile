package com.sciencehackfest.trive_sci.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.sciencehackfest.trive_sci.R
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationBar: BottomNavigationView {
	private var activeBackground: Drawable
	private var inActiveBackground: Drawable
	constructor(context: Context): super(context)
	constructor(context: Context, attrs: AttributeSet): super(context, attrs)
	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

	init {
		activeBackground = ContextCompat.getDrawable(context, R.drawable.bottom_nav_active) as Drawable
		inActiveBackground = ContextCompat.getDrawable(context, R.drawable.bottom_nav_inactive) as Drawable
		setupItemBackground()
	}

	private fun setupItemBackground() {
		for (i in 0 until menu.size()) {
			val item = menu.getItem(i)
			val itemView = findViewById<BottomNavigationItemView>(item.itemId)
			itemView?.background = if (item.isChecked) activeBackground else inActiveBackground
		}
	}

	override fun setItemActiveIndicatorColor(csl: ColorStateList?) {
		super.setItemActiveIndicatorColor(csl)
		setupItemBackground()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		setupItemBackground()
	}
}