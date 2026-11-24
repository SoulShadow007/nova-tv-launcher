package com.novatv.launcher.ui

import android.content.Context
import com.novatv.launcher.R
import com.novatv.launcher.model.AppInfo
import java.util.Locale
import kotlin.random.Random

/**
 * Deterministic pseudo-content shown when a favorite app receives focus:
 * recommendations, continue-watching, live viewers count and weekly novelties.
 * Values are seeded by the package name so they stay stable per app.
 */
object PreviewData {

    data class Preview(
        val recommended: String,
        val continueWatching: String,
        val viewersValue: String,
        val newItems: String
    )

    private val GENRES = listOf(
        "фильмов и сериалов", "сериалов", "аниме и кино",
        "документальных фильмов", "спортивных трансляций", "новинок недели"
    )

    private val SHOWS = listOf(
        "Полярис", "Тихий океан", "Северное сияние", "Последний рубеж",
        "Секретный проект", "Грань", "Селенга", "Кассиопея", "Вектор",
        "Аргонавты", "Кратер", "Меридиан"
    )

    private val NEW_WORDS = listOf("серий", "фильмов", "выпусков", "эпизодов")

    fun build(context: Context, app: AppInfo): Preview {
        val rnd = Random(app.packageName.hashCode().toLong() * 31L + 7L)
        val genre = GENRES[rnd.nextInt(GENRES.size)]
        val count = 80 + rnd.nextInt(500)
        val show = SHOWS[rnd.nextInt(SHOWS.size)]
        val season = 1 + rnd.nextInt(7)
        val episode = 1 + rnd.nextInt(24)
        val viewers = 10_000 + rnd.nextInt(990_000)
        val newCount = 5 + rnd.nextInt(40)
        val newWord = NEW_WORDS[rnd.nextInt(NEW_WORDS.size)]
        return Preview(
            recommended = context.getString(R.string.preview_recommended_value, count, genre),
            continueWatching = "«$show», сезон $season, серия $episode",
            viewersValue = context.getString(
                R.string.preview_viewers_value,
                String.format(Locale.GERMAN, "%,d", viewers).replace('.', ' ')
            ),
            newItems = context.getString(R.string.preview_new_value, newCount, newWord)
        )
    }
}
