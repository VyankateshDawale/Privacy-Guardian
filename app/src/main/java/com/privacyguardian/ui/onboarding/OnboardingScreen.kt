package com.privacyguardian.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguardian.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val gradient: List<Color>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            Icons.Default.Search,
            "Detect",
            "Find hidden risks",
            "On-device OCR scans screenshots, images & documents for API keys, passwords, OTP, cards, IDs — without uploading to cloud.",
            listOf(Safe, AccentIqoo)
        ),
        OnboardingPage(
            Icons.Default.AutoAwesome,
            "Understand",
            "Context-aware, not just regex",
            "Your OTP is 482913 → Critical. Order #482913 → harmless. We understand context before raising alarms.",
            listOf(AccentIqoo, AccentPurple)
        ),
        OnboardingPage(
            Icons.Default.Shield,
            "Protect",
            "Before you share",
            "One tap creates a safe copy — full redaction or smart mask keeps structure (j***@iqoo.com) while hiding danger.",
            listOf(SafeGradientStart, SafeGradientEnd)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { idx ->
            val page = pages[idx]
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(page.gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(page.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(page.title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text(page.subtitle, color = page.gradient.first(), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(page.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                val isSelected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 24.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Safe else Border)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onFinish) {
                Text("Skip", color = TextTertiary, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Safe, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next", fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        OnboardingScreen(onFinish = onDismiss)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
