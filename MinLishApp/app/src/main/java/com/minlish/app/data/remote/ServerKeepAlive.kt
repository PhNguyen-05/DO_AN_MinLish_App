package com.minlish.app.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ServerKeepAlive {
    private const val INTERVAL_MS = 10 * 60 * 1000L

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    RetrofitClient.instance.keepAlive()
                }
                delay(INTERVAL_MS)
            }
        }
    }
}
