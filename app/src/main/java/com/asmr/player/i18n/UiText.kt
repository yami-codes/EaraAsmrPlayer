package com.asmr.player.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    fun resolve(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> {
            if (args.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *args.toTypedArray())
            }
        }
    }

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> {
            if (args.isEmpty()) {
                stringResource(resId)
            } else {
                stringResource(resId, *args.toTypedArray())
            }
        }
    }
}

fun uiText(@StringRes resId: Int, vararg args: Any): UiText =
    UiText.Resource(resId, args.toList())

fun uiText(value: String): UiText = UiText.Dynamic(value)
