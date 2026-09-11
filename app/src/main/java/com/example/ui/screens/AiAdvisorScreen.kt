package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiOptimizationReport
import com.example.data.model.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiAdvisorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val aiReport by viewModel.aiReport.collectAsState()
    val isOptimizing by viewModel.isAiOptimizing.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isChatGenerating.collectAsState()

    var userQuery by remember { mutableStateOf("") }
    val completedActions = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Optimization Trigger Banner
        item {
            AiOptimizationHeroBanner(
                isOptimizing = isOptimizing,
                hasReport = aiReport != null,
                onRunOptimization = { viewModel.runAiOptimization() }
            )
        }

        // Display AI Report Diagnostics if generated
        aiReport?.let { report ->
            item {
                HealthScoreCard(report = report)
            }

            item {
                ReportSectionCard(
                    icon = Icons.Default.Tune,
                    title = "Channel Optimization & Interference",
                    content = report.optimalChannelRecommendation,
                    accentColor = CyberCyan
                )
            }

            item {
                ReportSectionCard(
                    icon = Icons.Default.Router,
                    title = "Router Placement & Antenna Angles",
                    content = report.antennaAndPlacementTip,
                    accentColor = CyberTeal
                )
            }

            item {
                ReportSectionCard(
                    icon = Icons.Default.Security,
                    title = "Network Security & Rogue Access Audit",
                    content = report.securityAudit,
                    accentColor = if (report.securityAudit.contains("CRITICAL", ignoreCase = true)) CyberRed else CyberGreen
                )
            }

            item {
                ReportSectionCard(
                    icon = Icons.Default.Wifi,
                    title = "Band Steering Recommendation",
                    content = report.bandSteeringAdvice,
                    accentColor = CyberPurple
                )
            }

            if (report.actionItems.isNotEmpty()) {
                item {
                    ActionChecklistCard(
                        actions = report.actionItems,
                        completedActions = completedActions,
                        onToggle = { action ->
                            if (completedActions.contains(action)) {
                                completedActions.remove(action)
                            } else {
                                completedActions.add(action)
                            }
                        }
                    )
                }
            }
        }

        // Interactive AI Wi-Fi Consultant Chat
        item {
            ChatSectionHeader()
        }

        // Quick Prompt Suggestions
        item {
            QuickPromptsRow(
                onPromptSelect = { prompt ->
                    userQuery = prompt
                    viewModel.sendChatMessage(prompt)
                    userQuery = ""
                }
            )
        }

        // Chat conversation bubbles
        items(chatMessages, key = { it.id }) { msg ->
            ChatBubble(message = msg)
        }

        if (isGenerating) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = CyberCyan,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini AI is analyzing wireless telemetry...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Chat input field
        item {
            ChatInputField(
                value = userQuery,
                onValueChange = { userQuery = it },
                onSend = {
                    if (userQuery.isNotBlank()) {
                        val text = userQuery
                        userQuery = ""
                        viewModel.sendChatMessage(text)
                    }
                },
                isGenerating = isGenerating
            )
        }
    }
}

@Composable
private fun AiOptimizationHeroBanner(
    isOptimizing: Boolean,
    hasReport: Boolean,
    onRunOptimization: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(18.dp)
            .testTag("ai_optimization_hero")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini AI Network Optimizer",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time RF synthesis, channel interference & intruder audit",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRunOptimization,
                enabled = !isOptimizing,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_ai_optimization_button")
            ) {
                if (isOptimizing) {
                    CircularProgressIndicator(
                        color = CyberSurface,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synthesizing Network Diagnostics...", color = CyberSurface, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = CyberSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasReport) "Re-Analyze Network Health" else "Run AI Wi-Fi Optimization",
                        color = CyberSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthScoreCard(report: AiOptimizationReport) {
    val scoreColor = when {
        report.overallHealthScore >= 80 -> CyberGreen
        report.overallHealthScore >= 60 -> CyberAmber
        else -> CyberRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OVERALL NETWORK HEALTH",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.summary,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.15f))
                    .border(2.dp, scoreColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${report.overallHealthScore}",
                        color = scoreColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "/100",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCard(
    icon: ImageVector,
    title: String,
    content: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ActionChecklistCard(
    actions: List<String>,
    completedActions: List<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Optimization Action Items",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        actions.forEach { action ->
            val isDone = completedActions.contains(action)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle(action) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isDone) CyberGreen else CyberSurfaceElevated)
                        .border(1.dp, if (isDone) CyberGreen else CyberBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = action,
                    color = if (isDone) TextMuted else TextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ChatSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Interactive Wi-Fi Consultant",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuickPromptsRow(onPromptSelect: (String) -> Unit) {
    val presets = listOf(
        "How do I eliminate dead zones?",
        "How to block an unauthorized host on router?",
        "Why is 5GHz faster but weaker?",
        "Should I separate 2.4GHz and 5GHz SSIDs?"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(presets) { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                    .clickable { onPromptSelect(preset) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = preset,
                    color = CyberTeal,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
                .border(
                    1.dp,
                    if (isUser) CyberCyan.copy(alpha = 0.4f) else CyberBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "You" else "WiFi Sentinel AI",
                    color = if (isUser) CyberCyan else CyberTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Ask about router config, signal, intruders...", fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = CyberSurfaceElevated,
                unfocusedContainerColor = CyberSurfaceElevated
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("ai_query_input")
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && !isGenerating,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (value.isNotBlank() && !isGenerating) CyberCyan else CyberSurfaceElevated)
                .testTag("send_ai_query_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (value.isNotBlank() && !isGenerating) CyberSurface else TextMuted
            )
        }
    }
}
