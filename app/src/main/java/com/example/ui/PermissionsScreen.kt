package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GpuDriverInfo
import com.example.data.model.PermissionItemState
import com.example.data.model.TermuxEnvironmentState
import com.example.data.termux.TermuxBridge
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostText
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WarmAmber

@Composable
fun PermissionsScreen(
    termuxEnv: TermuxEnvironmentState,
    gpuDriverInfo: GpuDriverInfo,
    permissions: List<PermissionItemState>,
    onRequestPermission: (PermissionItemState) -> Unit,
    onRequestAllHardwarePermissions: () -> Unit,
    onDownloadTermuxFDroid: () -> Unit,
    onDownloadTermuxApiFDroid: () -> Unit,
    onDownloadTermuxX11: () -> Unit,
    onCopyBootstrap: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Termux Addon, GPU & Permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    color = FrostText
                )
                Text(
                    text = "Package com.termux.studio • Folder sharing, GPU driver bridge, and hardware permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
            }
        }

        // Folder Sharing & Termux Addon Architecture Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurface)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = ElectricCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "How Folder Sharing Works (Termux:X11 Style)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan
                    )
                }
                Text(
                    text = "Termux studio uses package com.termux.studio without forcing a sharedUserId key lock (so it installs cleanly alongside F-Droid Termux). Folders are shared directly via Termux's com.termux.documents SAF provider (\$HOME) and ~/storage/shared (/sdcard) via termux-setup-storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
            }
        }

        // GPU API Drivers Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurface)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = MintEmerald)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "GPU API Drivers (Active Automatically)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MintEmerald
                    )
                }
                Text("${gpuDriverInfo.vendor} • ${gpuDriverInfo.renderer}", style = MaterialTheme.typography.bodyMedium, color = FrostText)
                Text(
                    text = "GLES: ${gpuDriverInfo.glesVersion} | Vulkan: ${gpuDriverInfo.vulkanVersion} • Driver: ${gpuDriverInfo.recommendedTermuxDriver}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate
                )
            }
        }

        // Termux Status & Bootstrap Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurface)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (termuxEnv.isTermuxInstalled) MintEmerald else WarmAmber
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (termuxEnv.isTermuxInstalled) {
                            "Termux Detected (${termuxEnv.termuxVersionName ?: "installed"})"
                        } else {
                            "Termux App Not Installed"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (termuxEnv.isTermuxInstalled) MintEmerald else WarmAmber
                    )
                }

                Text(
                    text = "Run inside Termux once to install GCC, Clang, Java, Rust, Go, Zig, Raylib, Vulkan/Zink GPU drivers, and enable external app execution:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Text(
                    text = TermuxBridge.BOOTSTRAP_SETUP_COMMAND,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GraphiteElevated)
                        .padding(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCopyBootstrap,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianCanvas),
                        modifier = Modifier.testTag("copy_bootstrap_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Setup")
                    }
                    OutlinedButton(
                        onClick = onDownloadTermuxFDroid,
                        modifier = Modifier.testTag("fdroid_termux_btn")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("F-Droid Termux")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!termuxEnv.isTermuxApiInstalled) {
                        OutlinedButton(onClick = onDownloadTermuxApiFDroid) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Termux:API")
                        }
                    }
                    if (!termuxEnv.isTermuxX11Installed) {
                        OutlinedButton(onClick = onDownloadTermuxX11) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Termux:X11")
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onRequestAllHardwarePermissions,
                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = ObsidianCanvas),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_all_hardware_btn")
            ) {
                Text("Grant Runtime Hardware Permissions (Camera, Mic, Bluetooth, Location, Notifications)")
            }
        }

        items(permissions, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CarbonSurface)
                    .border(
                        width = 1.dp,
                        color = if (item.isGranted) MintEmerald.copy(alpha = 0.5f) else SlateBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.category, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = FrostText)
                    Spacer(Modifier.height(2.dp))
                    Text(item.description, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                }
                Spacer(Modifier.width(10.dp))
                if (item.isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = MintEmerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Granted", style = MaterialTheme.typography.labelMedium, color = MintEmerald, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onRequestPermission(item) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("grant_perm_${item.id}")
                    ) {
                        Text("Grant")
                    }
                }
            }
        }
    }
}
