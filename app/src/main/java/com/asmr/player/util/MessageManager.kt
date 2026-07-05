package com.asmr.player.util

import android.content.Context
import androidx.annotation.StringRes
import com.asmr.player.i18n.UiText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class MessageType {
    Success,
    Error,
    Info,
    Warning
}

data class AppMessage(
    val id: Long,
    val message: String,
    val type: MessageType = MessageType.Info,
    val durationMs: Long = 2000,
    val createdAtMs: Long
) {
    fun formatForSnackbar(): String = "[${type.name.uppercase()}]$message"
}

@Singleton
class MessageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val seq = AtomicLong(0L)
    private val consumedMessageId = AtomicLong(0L)
    private val _messages = MutableSharedFlow<AppMessage>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages = _messages.asSharedFlow()

    fun showMessage(message: String, type: MessageType = MessageType.Info, durationMs: Long = 2000) {
        val normalizedMessage = when (type) {
            MessageType.Error -> AppErrorMessageFormatter.sanitize(message, context)
            else -> message.trim()
        }
        if (normalizedMessage.isBlank()) return
        emitMessage(normalizedMessage, type, durationMs)
    }

    fun showMessage(text: UiText, type: MessageType = MessageType.Info, durationMs: Long = 2000) {
        showMessage(text.resolve(context), type, durationMs)
    }

    fun showMessage(@StringRes messageRes: Int, vararg args: Any, type: MessageType = MessageType.Info, durationMs: Long = 2000) {
        val raw = if (args.isEmpty()) {
            context.getString(messageRes)
        } else {
            context.getString(messageRes, *args)
        }
        showMessage(raw, type, durationMs)
    }

    fun showSuccess(message: String) = showMessage(message, MessageType.Success)
    fun showSuccess(@StringRes messageRes: Int, vararg args: Any) = showMessage(messageRes, *args, type = MessageType.Success)
    fun showSuccess(text: UiText) = showMessage(text, MessageType.Success)

    fun showError(message: String) = showMessage(message, MessageType.Error)
    fun showError(@StringRes messageRes: Int, vararg args: Any) = showMessage(messageRes, *args, type = MessageType.Error)
    fun showError(text: UiText) = showMessage(text, MessageType.Error)

    fun showWarning(message: String) = showMessage(message, MessageType.Warning)
    fun showWarning(@StringRes messageRes: Int, vararg args: Any) = showMessage(messageRes, *args, type = MessageType.Warning)
    fun showWarning(text: UiText) = showMessage(text, MessageType.Warning)

    fun showInfo(message: String) = showMessage(message, MessageType.Info)
    fun showInfo(@StringRes messageRes: Int, vararg args: Any) = showMessage(messageRes, *args, type = MessageType.Info)
    fun showInfo(text: UiText) = showMessage(text, MessageType.Info)

    private fun emitMessage(normalizedMessage: String, type: MessageType, durationMs: Long) {
        val now = System.currentTimeMillis()
        val id = seq.incrementAndGet()
        _messages.tryEmit(
            AppMessage(
                id = id,
                message = normalizedMessage,
                type = type,
                durationMs = durationMs,
                createdAtMs = now
            )
        )
    }

    fun tryConsume(messageId: Long): Boolean {
        while (true) {
            val current = consumedMessageId.get()
            if (messageId <= current) return false
            if (consumedMessageId.compareAndSet(current, messageId)) {
                return true
            }
        }
    }
}
