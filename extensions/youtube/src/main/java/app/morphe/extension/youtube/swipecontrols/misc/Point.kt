package app.morphe.extension.youtube.swipecontrols.misc

import android.view.MotionEvent

/**
 * A simple 2D point class.
 */
data class Point(
    val x: Int,
    val y: Int,
) {
    companion object {
        /**
         * Java callers get the point from a motion event through this, since an extension
         * function is not visible to them.
         */
        @JvmStatic
        fun fromMotionEvent(event: MotionEvent): Point =
            Point(event.x.toInt(), event.y.toInt())
    }
}

/**
 * Converts the motion event coordinates to a point.
 */
fun MotionEvent.toPoint(): Point =
    Point(x.toInt(), y.toInt())
