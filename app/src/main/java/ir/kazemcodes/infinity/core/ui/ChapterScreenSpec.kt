package ir.kazemcodes.infinity.core.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import ir.kazemcodes.infinity.feature_detail.presentation.chapter_detail.ChapterDetailScreen

object ChapterScreenSpec : ScreenSpec {

    override val navHostRoute: String = "chapter_detail_route/{bookId}/{sourceId}"

    override val arguments: List<NamedNavArgument> = listOf(
        NavigationArgs.sourceId,
        NavigationArgs.bookId,
    )

    fun buildRoute(
        bookId: Int,
        sourceId: Long,
    ): String {
        return "chapter_detail_route/$bookId/$sourceId"
    }



    @OptIn(ExperimentalPagerApi::class, androidx.compose.animation.ExperimentalAnimationApi::class,
        androidx.compose.material.ExperimentalMaterialApi::class)
    @Composable
    override fun Content(
        navController: NavController,
        navBackStackEntry: NavBackStackEntry,
        scaffoldState: ScaffoldState
    ) {
        ChapterDetailScreen(navController = navController)
    }

}