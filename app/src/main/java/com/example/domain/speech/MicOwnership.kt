package com.example.domain.speech

import java.util.concurrent.atomic.AtomicReference

enum class MicOwner {
    NONE,
    UI,
    WAKE_WORD
}

/**
 * Process-global interlock ensuring the UI's SpeechManager and the background service's
 * WakeWordEngine never hold the microphone/SpeechRecognizer at the same time - two concurrent
 * SpeechRecognizer instances on the same process reliably produce ERROR_RECOGNIZER_BUSY.
 */
object MicOwnership {

    private val owner = AtomicReference(MicOwner.NONE)

    /** Succeeds only if the mic is free or already held by [who]. */
    fun tryClaim(who: MicOwner): Boolean {
        return owner.compareAndSet(MicOwner.NONE, who) || owner.get() == who
    }

    /** Unconditionally takes ownership - used by the UI, which always wins over the wake loop. */
    fun forceClaim(who: MicOwner) {
        owner.set(who)
    }

    /** No-op if [who] is not the current owner, so a stale/late release can't steal ownership. */
    fun release(who: MicOwner) {
        owner.compareAndSet(who, MicOwner.NONE)
    }

    fun current(): MicOwner = owner.get()
}
