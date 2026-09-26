package com.apexus.storagelens.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.R
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: ImageVector, val title: Int, val text: Int)

private val PAGES = listOf(
    OnboardingPage(Icons.Filled.Storage, R.string.onboarding_1_title, R.string.onboarding_1_text),
    OnboardingPage(Icons.Filled.CleaningServices, R.string.onboarding_2_title, R.string.onboarding_2_text),
    OnboardingPage(Icons.Filled.Security, R.string.onboarding_3_title, R.string.onboarding_3_text),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = PAGES[index]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(page.icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(page.text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(PAGES.size) { i ->
                    val color = if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    Box(Modifier.padding(4.dp).size(10.dp).clip(CircleShape).background(color))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onFinished) { Text(stringResource(R.string.action_skip)) }
                Button(onClick = {
                    if (pager.currentPage < PAGES.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onFinished()
                }) {
                    Text(stringResource(if (pager.currentPage < PAGES.lastIndex) R.string.action_next else R.string.action_continue))
                }
            }
        }
    }
}
