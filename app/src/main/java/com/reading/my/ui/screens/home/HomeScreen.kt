package com.reading.my.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onNavigateToBookshelf: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToReader: (Long) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "首页（待开发）",
            color = TextPrimary
        )
    }
}
