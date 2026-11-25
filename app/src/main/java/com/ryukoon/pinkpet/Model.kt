package com.ryukoon.pinkpet

enum class Form { NORMAL, POWER, CHICKEN }

/** 한 프레임의 자세. 좌표 단위는 24x24 도트 그리드. */
data class Pose(
    var hx: Int = 0, var hy: Int = 0, var by: Int = 12, var ly: Int = 18,
    var legs: String = "stand", var aL: String = "down", var aR: String = "down",
    var eyes: String = "open", var look: Int = 1, var mouth: String = "flat",
    var blush: Boolean = false, var lying: Boolean = false, var pot: Boolean = false,
    var eat: Boolean = false, var pen: String? = null, var box: Boolean = false,
    var prop: String? = null, var lift: Float = 0f, var pivot: Float = 0f,
    var sx: Float = 1f, var sy: Float = 1f, var rot: Float = 0f
)

