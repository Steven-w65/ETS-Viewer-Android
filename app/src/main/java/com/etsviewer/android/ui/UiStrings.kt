package com.etsviewer.android.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Injectable so feedback rules remain testable on the JVM without a global Android Context. */
fun interface UiStrings {
    fun text(@StringRes id: Int, vararg args: Any): String
}

class AndroidUiStrings(private val resources: Resources) : UiStrings {
    override fun text(id: Int, vararg args: Any): String = resources.getString(id, *args)
}

@Composable fun rememberUiStrings(): UiStrings {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) { AndroidUiStrings(resources) }
}
