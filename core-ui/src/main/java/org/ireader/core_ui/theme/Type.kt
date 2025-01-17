package org.ireader.core_ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

fun getDefaultFont():FontType {
    return  FontType("Roboto", FontFamily.Default)
}


val readerScreenBackgroundColors = listOf<BackgroundColor>(
    BackgroundColor.Black,
    BackgroundColor.White,
    BackgroundColor.GrayishBlack,
    BackgroundColor.CloudColor,
    BackgroundColor.Gray88,
    BackgroundColor.CornflowerBlue,
    BackgroundColor.NewCar,
    BackgroundColor.AntiqueWhite,
    BackgroundColor.LavenderRose,
    BackgroundColor.ArylideYellow,
    BackgroundColor.DesertSand,
    BackgroundColor.Lavender,
    BackgroundColor.PastelPink,
    BackgroundColor.PastelYellow,
)

sealed class BackgroundColor(val color: Color, val onTextColor: Color, val index: Int) {
    object Black : BackgroundColor(Color(0xff000000), Color(0xffffffff), 0)
    object White : BackgroundColor(Color(0xffffffff), Color(0xff000000), 1)
    object GrayishBlack : BackgroundColor(Color(0xff262626), Color(0xFFE9E9E9), 2)
    object CloudColor : BackgroundColor(Color(0xFF405A61), Color(0xFFFFFFFF), 3)
    object Gray88 : BackgroundColor(Color(248, 249, 250), Color(51, 51, 51), 4)
    object CornflowerBlue : BackgroundColor(Color(150, 173, 252), Color(0xff000000), 5)
    object NewCar : BackgroundColor(Color(219, 225, 241), Color(0xff000000), 6)
    object AntiqueWhite : BackgroundColor(Color(237, 221, 110), Color(0xff000000), 7)
    object LavenderRose : BackgroundColor(Color(168, 242, 154), Color(0xff000000), 8)
    object ArylideYellow : BackgroundColor(Color(233, 214, 107), Color(0xff000000), 9)
    object DesertSand : BackgroundColor(Color(237, 209, 176), Color(0xff000000), 10)
    object Lavender : BackgroundColor(Color(185, 135, 220), Color(0xff000000), 11)
    object PastelPink : BackgroundColor(Color(224, 166, 170), Color(0xff000000), 12)
    object PastelYellow : BackgroundColor(Color(248, 253, 137), Color(0xff000000), 13)
}
