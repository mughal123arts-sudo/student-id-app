package com.school.studentid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.school.studentid.R
import com.school.studentid.ui.theme.AppBackground
import kotlinx.coroutines.delay

/**
 * Shown for a couple of seconds when the app launches, then automatically
 * continues to Setup (first run) or Login.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "App logo",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .padding(24.dp)
            )
        }
    }
}
