package com.reading.my.ui.navigation

/**
 * 路由定义
 *
 * 导航架构：
 *   Login（独立页）
 *     ↓ 登录成功
 *   Main（底部导航容器，含4个Tab）
 *     ├── Bookshelf  （书架）- 默认首页
 *     ├── Bookstore   （书库/发现）
 *     ├── Community  （同好/圈子）
 *     └── Profile    （我的）
 */
sealed class Screen(val route: String) {
    /** 登录页面（独立路由，不在底部导航内） */
    data object Login : Screen("login")

    /** 主界面（底部导航容器） */
    data object Main : Screen("main")

    // ==================== 底部导航 Tab 路由（嵌套在 Main 内） ====================

    /** 书架 - 默认Tab */
    data object Bookshelf : Screen("bookshelf")

    /** 书库/发现 */
    data object Bookstore : Screen("bookstore")

    /** 同好/圈子 */
    data object Community : Screen("community")

    /** 我的 */
    data object Profile : Screen("profile")
}

/**
 * 底部导航项定义
 *
 * @param route 对应的 Screen.route
 * @param title 显示标题
 * @param icon 未选中时的图标描述（用于绘制/图标选择）
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: String,        // 图标名称或 emoji
) {
    data object Bookshelf : BottomNavItem(Screen.Bookshelf.route, "书架", "bookshelf")
    data object Bookstore : BottomNavItem(Screen.Bookstore.route, "书库", "bookstore")
    data object Community : BottomNavItem(Screen.Community.route, "同好", "community")
    data object Profile : BottomNavItem(Screen.Profile.route, "我的", "profile")

    companion object {
        /** 所有底部导航项列表 */
        val tabs = listOf(Bookshelf, Bookstore, Community, Profile)
    }
}
