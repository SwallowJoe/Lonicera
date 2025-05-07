package com.android.lonicera.components.widget

enum class Direction {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

fun Direction.reverse() = when (this) {
    Direction.TOP -> Direction.BOTTOM
    Direction.BOTTOM -> Direction.TOP
    Direction.LEFT -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}