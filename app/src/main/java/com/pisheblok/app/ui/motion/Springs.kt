package com.pisheblok.app.ui.motion

/**
 * Именованные пружины — порт констант Motion UI (SmartESS ETA / Motion-KIT).
 * Кит задаёт k/c (жёсткость/демпфирование); в Compose это
 * stiffness + dampingRatio = c / (2·√k), значения пересчитаны:
 *
 *  пилюля ведущий край  620/40 → ζ 0.80, stiffness 620
 *  пилюля хвостовой     170/19 → ζ 0.73, stiffness 170
 *  желе-палец           900/50 → ζ 0.83, stiffness 900
 *  точки графиков       200/17 → ζ 0.60, stiffness 200
 *  отскоки/иконки       320/18 → ζ 0.50, stiffness 320
 */
object BlokMotion {

    // Пилюля таб-бара: ведущий край жёсткий, хвостовой мягкий (растяжение на ходу).
    const val PILL_LEAD_STIFFNESS = 620f
    const val PILL_LEAD_DAMPING = 0.80f
    const val PILL_TAIL_STIFFNESS = 170f
    const val PILL_TAIL_DAMPING = 0.73f

    // Желе-палец слайдера количества.
    const val THUMB_STIFFNESS = 900f
    const val THUMB_DAMPING = 0.83f

    // Точки графиков / кольца свежести.
    const val CURVE_STIFFNESS = 200f
    const val CURVE_DAMPING = 0.60f

    // Отскоки: иконки навигации, дёрганье hold-кнопки.
    const val BOUNCY_STIFFNESS = 320f
    const val BOUNCY_DAMPING = 0.50f

    // Общий мягкий отклик (нажатия, влёты).
    const val SOFT_STIFFNESS = 500f
    const val SOFT_DAMPING = 0.80f

    // Каскад страниц: дети × 45ms + 70ms (аналог --i × 45ms + 70ms из кита).
    const val STAGGER_STEP_MS = 45
    const val STAGGER_BASE_MS = 70

    fun staggerMs(index: Int): Int = STAGGER_BASE_MS + index * STAGGER_STEP_MS

    // Удержание hold-кнопки.
    const val HOLD_MS = 1200
}
