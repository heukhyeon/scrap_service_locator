package io.anonymous.widget.util

import android.os.Looper
import androidx.annotation.VisibleForTesting


/**
 * JUnit 테스트시 UI Thread 여부 검증 회피용
 */
@VisibleForTesting
internal var skipThreadCheck = false

fun requireIsUIThread() {
    if (skipThreadCheck) return
    if (Looper.myLooper() != Looper.getMainLooper()) {
        throw IllegalStateException("Expected UI Thread But Actual Worker Thread")
    }
}

fun requireIsWorkerThread() {
    if (skipThreadCheck) return
    if (Looper.myLooper() == Looper.getMainLooper()) {
        throw IllegalStateException("Expected Worker Thread But Actual UI Thread")
    }
}