package dev.gtcl.astro.ui.fragments

import android.view.View
import androidx.annotation.RequiresApi
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

private const val MIN_SCALE = 0.85f
private const val MIN_ALPHA = 0.5f

@RequiresApi(21)
class SlidePageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    val scaleFactor = max(MIN_SCALE, 1 - abs(position))
//                    translationZ = -1f
                    alpha = (MIN_ALPHA + (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                    // Counteract the default slide transition
                    translationX = pageWidth * -position
                }
                position <= 1 -> { // (0,1]
//                    translationZ = 0f
                    // Use the default slide transition when moving to the left page
                    alpha = 1f
                    translationX = 0f
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}