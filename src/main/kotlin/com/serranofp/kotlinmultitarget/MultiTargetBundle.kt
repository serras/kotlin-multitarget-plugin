package com.serranofp.kotlinmultitarget

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object MultiTargetBundle {
    @NonNls
    const val BUNDLE: String = "messages.MultiTargetBundle"

    @Nls @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        DynamicBundle(MultiTargetBundle::class.java, BUNDLE).getMessage(key, *params)
}