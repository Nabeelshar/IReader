package ir.kazemcodes.infinity.core.ui

import androidx.navigation.NavType
import androidx.navigation.navArgument
import ir.kazemcodes.infinity.core.utils.Constants.ARG_HIDE_BOTTOM_BAR

object NavigationArgs {
    val bookId = navArgument("bookId") {
        type = NavType.IntType
    }
    val sourceId = navArgument("sourceId") {
        type = NavType.LongType
    }
    val exploreType = navArgument("exploreType") {
        type = NavType.IntType
    }
    val url = navArgument("url") {
        type = NavType.StringType
        nullable = true
    }
    val fetchType = navArgument("fetchType") {
        type = NavType.IntType
    }
    val chapterId = navArgument("chapterId") {
        type = NavType.IntType
    }
    val showBottomNav = navArgument(ARG_HIDE_BOTTOM_BAR) {
        defaultValue = true
    }
}