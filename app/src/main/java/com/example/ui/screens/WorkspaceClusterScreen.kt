package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BetaFlightRunResult
import com.example.data.model.ClusterBackupSnapshot
import com.example.data.model.NodeClusterStatus
import com.example.data.model.PergamusClusterNode
import com.example.data.model.RecurringBetaTester
import com.example.data.model.TrialCategory
import com.example.data.model.TrialErrorEntry
import com.example.data.model.TrialOutcome
import com.example.data.model.WorkspaceProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRedGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WorkspaceClusterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val workspaces by viewModel.workspaces.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val pergamusNodes by viewModel.pergamusNodes.collectAsState()
    val backupSnapshots by viewModel.backupSnapshots.collectAsState()
    val trialEntries by viewModel.trialEntries.collectAsState()
    val betaTesters by viewModel.betaTesters.collectAsState()
    val lastFlightResult by viewModel.lastFlightResult.collectAsState()
    val isFlightRunning by viewModel.isFlightRunning.collectAsState()
    val isClusterSyncing by viewModel.isClusterSyncing.collectAsState()

    var activeSubTab by remember { mutableIntStateOf(0) }
    // 0: Native Backup & Clusters
    // 1: Nodes Pergamus
    // 2: Trial & Error Library (Sources-Data)
    // 3: Recurring Beta Testers

    // Dialog States
    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showRunTrialDialog by remember { mutableStateOf(false) }
    var showEnrollTesterDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFlightResultDialog by remember { mutableStateOf(false) }
    var inspectingSnapshot by remember { mutableStateOf<ClusterBackupSnapshot?>(null) }
    var inspectingTrial by remember { mutableStateOf<TrialErrorEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        // Global Opifex Organization Header
        GlobalOpifexHeader(
            workspace = currentWorkspace,
            workspaces = workspaces,
            onSelectWorkspace = { viewModel.selectWorkspace(it) },
            onBack = onBack
        )

        // Sub-Navigation Segmented Bar
        WorkspaceSubTabBar(
            selectedTab = activeSubTab,
            onSelectTab = { activeSubTab = it },
            backupCount = backupSnapshots.size,
            nodeCount = pergamusNodes.size,
            trialCount = trialEntries.size,
            testerCount = betaTesters.size
        )

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (activeSubTab) {
                0 -> NativeBackupClustersTab(
                    snapshots = backupSnapshots,
                    currentWorkspace = currentWorkspace,
                    nodes = pergamusNodes,
                    onCreateSnapshot = { showCreateBackupDialog = true },
                    onRestore = { snapshotId ->
                        val success = viewModel.restoreClusterSnapshot(snapshotId)
                        if (success) {
                            Toast.makeText(context, "Cluster snapshot restored successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onInspect = { inspectingSnapshot = it },
                    onDelete = { viewModel.deleteClusterSnapshot(it) }
                )
                1 -> NodesPergamusTab(
                    nodes = pergamusNodes,
                    isSyncing = isClusterSyncing,
                    onSyncAll = {
                        viewModel.syncAllPergamusNodes {
                            Toast.makeText(context, "All Pergamus nodes synchronized!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddNode = { showAddNodeDialog = true },
                    onToggleIsolate = { viewModel.toggleIsolatePergamusNode(it) }
                )
                2 -> TrialErrorLibraryTab(
                    entries = trialEntries,
                    onRunTrial = { showRunTrialDialog = true },
                    onInspect = { inspectingTrial = it },
                    onReRun = { entry ->
                        viewModel.runTrialSimulation(
                            title = entry.title,
                            category = entry.category,
                            protocol = entry.protocol,
                            sourceNode = entry.sourceNode
                        )
                        Toast.makeText(context, "Executed trial: ${entry.title}", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> RecurringBetaTestersTab(
                    testers = betaTesters,
                    isFlightRunning = isFlightRunning,
                    lastFlightResult = lastFlightResult,
                    onRunFlight = {
                        viewModel.runDailyBetaFlightSuite {
                            showFlightResultDialog = true
                        }
                    },
                    onEnrollTester = { showEnrollTesterDialog = true },
                    onSubmitFeedback = { showFeedbackDialog = true },
                    onViewLastFlight = { showFlightResultDialog = true }
                )
            }
        }
    }

    // --- Dialogs ---
    if (showCreateBackupDialog) {
        CreateNativeBackupDialog(
            workspaceName = currentWorkspace.name,
            nodeCount = pergamusNodes.size,
            onDismiss = { showCreateBackupDialog = false },
            onConfirm = { note ->
                viewModel.createNativeClusterBackup(note) { snap ->
                    Toast.makeText(context, "Native backup created: ${snap.backupId}", Toast.LENGTH_SHORT).show()
                }
                showCreateBackupDialog = false
            }
        )
    }

    if (showAddNodeDialog) {
        AddPergamusNodeDialog(
            onDismiss = { showAddNodeDialog = false },
            onConfirm = { name, ip, mac, role ->
                viewModel.addPergamusNode(name, ip, mac, role)
                Toast.makeText(context, "Node $name registered to cluster!", Toast.LENGTH_SHORT).show()
                showAddNodeDialog = false
            }
        )
    }

    if (showRunTrialDialog) {
        RunTrialSimulationDialog(
            nodes = pergamusNodes,
            onDismiss = { showRunTrialDialog = false },
            onConfirm = { title, category, protocol, node ->
                viewModel.runTrialSimulation(title, category, protocol, node)
                Toast.makeText(context, "Trial simulated & logged to source-data library!", Toast.LENGTH_SHORT).show()
                showRunTrialDialog = false
            }
        )
    }

    if (showEnrollTesterDialog) {
        EnrollBetaTesterDialog(
            onDismiss = { showEnrollTesterDialog = false },
            onConfirm = { name, email, device ->
                viewModel.enrollRecurringBetaTester(name, email, device)
                Toast.makeText(context, "Enrolled recurring beta tester: $name", Toast.LENGTH_SHORT).show()
                showEnrollTesterDialog = false
            }
        )
    }

    if (showFeedbackDialog) {
        SubmitBetaFeedbackDialog(
            testers = betaTesters,
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { testerId, feedback, attachLogs ->
                Toast.makeText(context, "Telemetry & recurring beta feedback logged!", Toast.LENGTH_SHORT).show()
                showFeedbackDialog = false
            }
        )
    }

    if (showFlightResultDialog && lastFlightResult != null) {
        BetaFlightResultDialog(
            result = lastFlightResult!!,
            onDismiss = { showFlightResultDialog = false }
        )
    }

    inspectingSnapshot?.let { snapshot ->
        InspectSnapshotDialog(
            snapshot = snapshot,
            onDismiss = { inspectingSnapshot = null },
            onCopyJson = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Opifex Backup JSON", snapshot.rawJsonPayload)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    inspectingTrial?.let { trial ->
        InspectTrialSourceDataDialog(
            entry = trial,
            onDismiss = { inspectingTrial = null }
        )
    }
}

// ==========================================
// 1. GLOBAL OPIFEX HEADER
// ==========================================
@Composable
private fun GlobalOpifexHeader(
    workspace: WorkspaceProfile,
    workspaces: List<WorkspaceProfile>,
    onSelectWorkspace: (String) -> Unit,
    onBack: () -> Unit
) {
    var expandedWorkspaceMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface)
            .border(1.dp, CyberBorder.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, CircleShape)
                        .testTag("btn_back_from_workspace")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Global Opifex Innovative Solutions",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OPX-9836",
                                color = CyberPurple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Full Stacking Workspaces • Nodes Pergamus Cluster",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Active Workspace Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { expandedWorkspaceMenu = !expandedWorkspaceMenu }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("btn_workspace_menu_toggle")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SecOps Active",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Expanded Workspace Dropdown Pill Bar
        if (expandedWorkspaceMenu) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Switch Full-Stack Workspace Tenant:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                workspaces.forEach { ws ->
                    val isSelected = ws.id == workspace.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                            .border(1.dp, if (isSelected) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectWorkspace(ws.id)
                                expandedWorkspaceMenu = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = ws.name,
                                color = if (isSelected) CyberCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${ws.tier} • ${ws.activeNodesCount} Nodes",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. SUB-NAVIGATION BAR
// ==========================================
@Composable
private fun WorkspaceSubTabBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    backupCount: Int,
    nodeCount: Int,
    trialCount: Int,
    testerCount: Int
) {
    val tabs = listOf(
        "💾 Native Backup ($backupCount)",
        "🌐 Nodes Pergamus ($nodeCount)",
        "📚 Trial & Error ($trialCount)",
        "👥 Recurring Beta ($testerCount)"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) CyberCyan.copy(alpha = 0.8f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectTab(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("tab_workspace_sub_$index")
            ) {
                Text(
                    text = label,
                    color = if (isSelected) CyberCyan else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ==========================================
// 3. TAB: NATIVE BACKUP & CLUSTERS
// ==========================================
@Composable
private fun NativeBackupClustersTab(
    snapshots: List<ClusterBackupSnapshot>,
    currentWorkspace: WorkspaceProfile,
    nodes: List<PergamusClusterNode>,
    onCreateSnapshot: () -> Unit,
    onRestore: (String) -> Unit,
    onInspect: (ClusterBackupSnapshot) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Full Stacking in Workspaces Native Backup
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CyberSurfaceElevated, CyberSurface)
                        )
                    )
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Full Stacking in Workspaces",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Native Backup with All Clusters",
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NATIVE READY",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Generates a cryptographically verified SHA-256 snapshot across all active nodes (Nodes Pergamus), device whitelists, zero-tolerance sentry rules, and protocol source-data trials.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCreateSnapshot,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_create_native_snapshot"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = CyberSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create Native Snapshot",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Cluster Backup Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupMetricPill(
                    label = "Cluster Scope",
                    value = "All Clusters",
                    accent = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                BackupMetricPill(
                    label = "Target Nodes",
                    value = "${nodes.size} Pergamus",
                    accent = CyberPurple,
                    modifier = Modifier.weight(1f)
                )
                BackupMetricPill(
                    label = "Snapshots",
                    value = "${snapshots.size} Verified",
                    accent = CyberGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section Title: Historical Cluster Snapshots
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cluster Backup Snapshots (${snapshots.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "SHA-256 Verified",
                    color = CyberGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(snapshots, key = { it.backupId }) { snapshot ->
            SnapshotCard(
                snapshot = snapshot,
                onRestore = { onRestore(snapshot.backupId) },
                onInspect = { onInspect(snapshot) },
                onDelete = { onDelete(snapshot.backupId) }
            )
        }
    }
}

@Composable
private fun BackupMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SnapshotCard(
    snapshot: ClusterBackupSnapshot,
    onRestore: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("card_snapshot_${snapshot.backupId}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CyberGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = snapshot.backupId,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = snapshot.formattedDate,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${snapshot.sizeKb} KB",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Target: ${snapshot.clusterTarget} • ${snapshot.nodeCount} Nodes • ${snapshot.trialsCount} Trials",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SHA-256: ${snapshot.checksumSha256.take(16)}...",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_restore_${snapshot.backupId}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_inspect_${snapshot.backupId}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Inspect JSON",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CyberRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. TAB: NODES PERGAMUS
// ==========================================
@Composable
private fun NodesPergamusTab(
    nodes: List<PergamusClusterNode>,
    isSyncing: Boolean,
    onSyncAll: () -> Unit,
    onAddNode: () -> Unit,
    onToggleIsolate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cluster Summary & Action Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberPurple.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = CyberPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Nodes Pergamus Cluster",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Distributed Zero-Trust Security Mesh",
                                    color = CyberPurple,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HEALTH 99.98%",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sync_all_pergamus_nodes"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPurple,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "Syncing..." else "Sync All Nodes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onAddNode,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_add_pergamus_node"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Register Node",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Cluster Nodes (${nodes.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Mesh Protocol: 802.11ax/be",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        items(nodes, key = { it.nodeId }) { node ->
            PergamusNodeCard(
                node = node,
                onToggleIsolate = { onToggleIsolate(node.nodeId) }
            )
        }
    }
}

@Composable
private fun PergamusNodeCard(
    node: PergamusClusterNode,
    onToggleIsolate: () -> Unit
) {
    val isIsolated = node.status == NodeClusterStatus.ISOLATED
    val statusColor = when (node.status) {
        NodeClusterStatus.ONLINE -> CyberGreen
        NodeClusterStatus.SYNCING -> CyberCyan
        NodeClusterStatus.STANDBY -> CyberYellow
        NodeClusterStatus.ISOLATED -> CyberRed
        NodeClusterStatus.HARDENED -> CyberPurple
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(
                1.dp,
                if (isIsolated) CyberRed.copy(alpha = 0.8f) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .testTag("card_node_${node.nodeId}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = node.status.label,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Role: ${node.role} • IP: ${node.ipAddress} • MAC: ${node.macAddress}",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latency: ${node.latencyMs}ms • Uptime: ${node.uptimePercent}%",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "${node.sourceDataCount} Sources • ${node.trialCount} Trials",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onToggleIsolate,
                    modifier = Modifier.testTag("btn_isolate_${node.nodeId}"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isIsolated) "Reconnect Node" else "Isolate Node",
                        color = if (isIsolated) CyberGreen else CyberRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. TAB: TRIAL & ERROR LIBRARY (SOURCES-DATA)
// ==========================================
@Composable
private fun TrialErrorLibraryTab(
    entries: List<TrialErrorEntry>,
    onRunTrial: () -> Unit,
    onInspect: (TrialErrorEntry) -> Unit,
    onReRun: (TrialErrorEntry) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TrialCategory.ALL) }

    val filteredEntries = remember(entries, selectedCategory) {
        if (selectedCategory == TrialCategory.ALL) entries
        else entries.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner: A trial or error library rich with sources-data
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Trial & Error Library",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Rich with Sources-Data & Packet Dumps",
                                    color = CyberCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onRunTrial,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = CyberSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_run_new_trial_sim")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run Trial",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Live repository capturing adversary attack vectors, RF handshake errors, 802.11ax/be packet traces, and raw frame hex dumps across the Pergamus cluster.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrialCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceElevated)
                            .border(1.dp, if (isSelected) CyberCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${category.iconText} ${category.label}",
                            color = if (isSelected) CyberCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        items(filteredEntries, key = { it.id }) { entry ->
            TrialEntryCard(
                entry = entry,
                onInspect = { onInspect(entry) },
                onReRun = { onReRun(entry) }
            )
        }
    }
}

@Composable
private fun TrialEntryCard(
    entry: TrialErrorEntry,
    onInspect: () -> Unit,
    onReRun: () -> Unit
) {
    val outcomeColor = when (entry.outcome) {
        TrialOutcome.PASSED -> CyberGreen
        TrialOutcome.MITIGATED -> CyberCyan
        TrialOutcome.REPRODUCED -> CyberYellow
        TrialOutcome.ISOLATED -> CyberRed
        TrialOutcome.FLAGGED -> CyberPurple
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("card_trial_${entry.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(outcomeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.outcome.label,
                        color = outcomeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Protocol: ${entry.protocol} • Node: ${entry.sourceNode}",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.rootCause,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Raw Hex Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberBackground)
                    .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = entry.sourceDataPayloadHex.take(48) + "...",
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.testIterations} Iterations Run",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInspect,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_inspect_trial_${entry.id}")
                    ) {
                        Text(
                            text = "View Source-Data",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onReRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan.copy(alpha = 0.2f),
                            contentColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Re-Run",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. TAB: RECURRING BETA TESTERS
// ==========================================
@Composable
private fun RecurringBetaTestersTab(
    testers: List<RecurringBetaTester>,
    isFlightRunning: Boolean,
    lastFlightResult: BetaFlightRunResult?,
    onRunFlight: () -> Unit,
    onEnrollTester: () -> Unit,
    onSubmitFeedback: () -> Unit,
    onViewLastFlight: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner: ### Recurring Beta Testers ###
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberPurple.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = CyberPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "### Recurring Beta Testers ###",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Global Opifex Flightdeck Ring 4.2",
                                    color = CyberPurple,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RING 4.2",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Continuous automated telemetry flight verification. Active recurring beta testers validate Pergamus node cluster health, ARP spoof tolerance, and native snapshot recovery daily.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunFlight,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_run_daily_flight_suite"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPurple,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFlightRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFlightRunning) "Running Flight..." else "Run Daily Flight Suite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onEnrollTester,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_enroll_beta_tester"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enroll Tester",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (lastFlightResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onViewLastFlight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📋 View Latest Flight Report (${lastFlightResult.testsExecuted} tests / 100% Passed)",
                                color = CyberGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action: Submit Beta Feedback
        item {
            OutlinedButton(
                onClick = onSubmitFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_submit_beta_feedback"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = CyberYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Recurring Beta Feedback & Telemetry",
                    color = CyberYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Recurring Testers Roster Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recurring Beta Testers Roster (${testers.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Streak Leaderboard",
                    color = CyberCyan,
                    fontSize = 10.sp
                )
            }
        }

        items(testers, key = { it.testerId }) { tester ->
            BetaTesterCard(tester = tester)
        }
    }
}

@Composable
private fun BetaTesterCard(tester: RecurringBetaTester) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("card_tester_${tester.testerId}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tester.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = tester.email,
                        color = CyberCyan,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tester.flightRank,
                        color = CyberPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Device: ${tester.devicePlatform}",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Notes: ${tester.notes}",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 ${tester.activeStreakDays} Day Streak • ${tester.iterationsCompleted} Iterations",
                    color = CyberGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = tester.lastActiveFormatted,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==========================================
// 7. MODALS & DIALOGS
// ==========================================

@Composable
private fun CreateNativeBackupDialog(
    workspaceName: String,
    nodeCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Create Native Cluster Backup",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Target Workspace: $workspaceName",
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Cluster: Nodes Pergamus ($nodeCount nodes enrolled)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Backup Note / Version Tag", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Pre-flight flightdeck sync", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberSurface)
            ) {
                Text("Generate Snapshot", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddPergamusNodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Register Pergamus Node",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Node Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Node Pergamus-Epsilon", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address", fontSize = 11.sp) },
                    placeholder = { Text("192.168.1.110", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address", fontSize = 11.sp) },
                    placeholder = { Text("70:EE:50:8A:21:49", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Cluster Role", fontSize = 11.sp) },
                    placeholder = { Text("Secondary Spectrum Radar", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, ip, mac, role) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = TextPrimary)
            ) {
                Text("Register Node", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun RunTrialSimulationDialog(
    nodes: List<PergamusClusterNode>,
    onDismiss: () -> Unit,
    onConfirm: (String, TrialCategory, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TrialCategory.SECURITY_TRIAL) }
    var protocol by remember { mutableStateOf("802.11ax / SAE Handshake") }
    var selectedNode by remember { mutableStateOf(nodes.firstOrNull()?.displayName ?: "Node Pergamus-Alpha") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Simulate New Security Trial",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Trial Title", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Rogue Beacon Jitter Injection", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = protocol,
                    onValueChange = { protocol = it },
                    label = { Text("Protocol Reference", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Text(
                    text = "Assign Target Node: $selectedNode",
                    color = CyberCyan,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, selectedCategory, protocol, selectedNode) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberSurface)
            ) {
                Text("Execute & Capture", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EnrollBetaTesterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Enroll Recurring Beta Tester",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", fontSize = 11.sp) },
                    placeholder = { Text("Liberty Kondo", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Tester Email", fontSize = 11.sp) },
                    placeholder = { Text("libertykondo9836@gmail.com", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = device,
                    onValueChange = { device = it },
                    label = { Text("Device Model & OS", fontSize = 11.sp) },
                    placeholder = { Text("Google Pixel 9 Pro (Android 14)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, device) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = TextPrimary)
            ) {
                Text("Enroll Tester", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SubmitBetaFeedbackDialog(
    testers: List<RecurringBetaTester>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Boolean) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    var attachLogs by remember { mutableStateOf(true) }
    var selectedTester by remember { mutableStateOf(testers.firstOrNull()?.testerId ?: "BT-OPX-104") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Submit Recurring Beta Feedback",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reporting as: ${testers.find { it.testerId == selectedTester }?.name ?: selectedTester}",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Observations, anomalies, or bug trace", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { attachLogs = !attachLogs }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (attachLogs) CyberGreen else CyberSurfaceElevated)
                            .border(1.dp, if (attachLogs) CyberGreen else CyberBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attach Pergamus Node Cluster Log Dump",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedTester, feedback, attachLogs) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberYellow, contentColor = CyberSurface)
            ) {
                Text("Submit Telemetry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun BetaFlightResultDialog(
    result: BetaFlightRunResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Beta Flight Certified",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Flight: ${result.flightId} • Duration: ${result.durationMs}ms",
                    color = CyberCyan,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .padding(10.dp)
                ) {
                    Text(
                        text = result.summaryReport,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberSurface)
            ) {
                Text("Acknowledge", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InspectSnapshotDialog(
    snapshot: ClusterBackupSnapshot,
    onDismiss: () -> Unit,
    onCopyJson: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "Native Cluster Snapshot JSON",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Snapshot: ${snapshot.backupId} (${snapshot.sizeKb} KB)",
                    color = CyberCyan,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = snapshot.rawJsonPayload,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCopyJson,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy JSON", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun InspectTrialSourceDataDialog(
    entry: TrialErrorEntry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = entry.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Protocol: ${entry.protocol} • Node: ${entry.sourceNode}",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Diagnostic Explanation:",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.rootCause,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Raw Source-Data Payload Dump:",
                    color = CyberGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberBackground)
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = entry.sourceDataPayloadHex,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
                Text(
                    text = "Summary: ${entry.sourceDataSummary}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberSurface)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
