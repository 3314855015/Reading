package com.reading.my.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Bookshelf : Screen("bookshelf")
    data object Search : Screen("search")
    data object Profile : Screen("profile")
    data object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: Long) = "reader/$bookId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: String
) {
    data object Home : BottomNavItem(Screen.Home.route, "首页", "🏠")
    data object Bookshelf : BottomNavItem(Screen.Bookshelf.route, "书架", "📚")
    data object Search : BottomNavItem(Screen.Search.route, "搜索", "🔍")
    data object Profile : BottomNavItem(Screen.Profile.route, "我的", "👤")
}
