
package org.ireader.common_models

import kotlinx.serialization.Serializable


data class LastReadChapter(
    val bookName: String,
    val source: String,
    val chapterLink: String,
    val chapterTitle: String,
)
