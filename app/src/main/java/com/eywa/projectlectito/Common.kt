package com.eywa.projectlectito

import android.view.View

fun Boolean.asVisibility() = if (this) View.VISIBLE else View.GONE

class Common {
}