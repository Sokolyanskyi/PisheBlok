package com.pisheblok.app.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Тикающий календарь для отсчётов дней до истечения срока.
 * Живёт отдельно от списков: когда день сменяется, рекомпозируются
 * только бейджи/кольца, подписанные на этот flow, — списки не трогаются.
 * Таймер активен лишь пока UI подписан (WhileSubscribed).
 */
object ExpiryTicker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    init {
        scope.launch {
            while (true) {
                delay(30_000)
                val now = LocalDate.now()
                if (now != _today.value) _today.value = now
            }
        }
    }
}
