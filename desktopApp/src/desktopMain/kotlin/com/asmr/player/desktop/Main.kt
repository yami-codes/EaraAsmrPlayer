package com.asmr.player.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.asmr.player.shared.i18n.SharedAppLanguage
import com.asmr.player.shared.platform.PlatformKind
import com.asmr.player.shared.platform.currentPlatform
import com.asmr.player.shared.platform.platformDisplayName

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Eara ASMR Player"
    ) {
        DesktopApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var language by remember { mutableStateOf(SharedAppLanguage.English) }
    val strings = remember(language) { DesktopStrings.forLanguage(language) }
    val platform = remember { currentPlatform() }
    val isDark = platform == PlatformKind.Windows || platform == PlatformKind.Linux

    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.appTitle) },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.welcomeTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(strings.welcomeSubtitle, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${strings.runningOn}: ${platformDisplayName()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Language, contentDescription = null)
                            Text(strings.languageSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SharedAppLanguage.entries.forEach { option ->
                                FilterChip(
                                    selected = language == option,
                                    onClick = { language = option },
                                    label = { Text(strings.languageLabel(option)) }
                                )
                            }
                        }
                    }
                }

                Card {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.platformSupport, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("• ${strings.platformWindows} — ${strings.platformWindowsNote}")
                        Text("• ${strings.platformLinux} — ${strings.platformLinuxNote}")
                        Text("• ${strings.platformIos} — ${strings.platformIosNote}")
                        Text("• ${strings.platformAndroid} — ${strings.platformAndroidNote}")
                    }
                }

                Text(
                    strings.androidFullFeatures,
                    modifier = Modifier.widthIn(max = 640.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class DesktopStrings(
    val appTitle: String,
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val runningOn: String,
    val languageSection: String,
    val platformSupport: String,
    val platformWindows: String,
    val platformWindowsNote: String,
    val platformLinux: String,
    val platformLinuxNote: String,
    val platformIos: String,
    val platformIosNote: String,
    val platformAndroid: String,
    val platformAndroidNote: String,
    val androidFullFeatures: String,
    val languageLabels: Map<SharedAppLanguage, String>
) {
    fun languageLabel(language: SharedAppLanguage): String =
        languageLabels[language] ?: language.wireValue

    companion object {
        fun forLanguage(language: SharedAppLanguage): DesktopStrings = when (language) {
            SharedAppLanguage.Thai -> thai()
            SharedAppLanguage.ChineseSimplified -> chinese()
            SharedAppLanguage.English, SharedAppLanguage.System -> english()
        }

        private fun english() = DesktopStrings(
            appTitle = "Eara ASMR Player",
            welcomeTitle = "Eara ASMR Player",
            welcomeSubtitle = "Cross-platform desktop companion",
            runningOn = "Running on",
            languageSection = "Language",
            platformSupport = "Platform support",
            platformWindows = "Windows",
            platformWindowsNote = "Primary desktop target with MSI installer",
            platformLinux = "Linux",
            platformLinuxNote = "DEB package and JVM desktop runtime",
            platformIos = "iOS",
            platformIosNote = "Shared Kotlin module compiles for iOS targets",
            platformAndroid = "Android",
            platformAndroidNote = "Full playback, downloads, and effects",
            androidFullFeatures = "Full playback features are available on Android. Desktop builds provide library management and settings sync.",
            languageLabels = mapOf(
                SharedAppLanguage.System to "System",
                SharedAppLanguage.English to "English",
                SharedAppLanguage.Thai to "Thai",
                SharedAppLanguage.ChineseSimplified to "Chinese (Simplified)"
            )
        )

        private fun thai() = english().copy(
            welcomeSubtitle = "แอปเดสก์ท็อปข้ามแพลตฟอร์ม",
            runningOn = "กำลังทำงานบน",
            languageSection = "ภาษา",
            platformSupport = "รองรับแพลตฟอร์ม",
            platformWindowsNote = "เป้าหมายเดสก์ท็อปหลัก พร้อมตัวติดตั้ง MSI",
            platformLinuxNote = "แพ็กเกจ DEB และ JVM desktop runtime",
            platformIosNote = "โมดูล Kotlin ร่วมคอมไพล์สำหรับ iOS",
            platformAndroidNote = "เล่นเต็มรูปแบบ ดาวน์โหลด และเอฟเฟกต์",
            androidFullFeatures = "ฟีเจอร์เล่นเต็มรูปแบบมีบน Android ส่วนเวอร์ชันเดสก์ท็อปรองรับการจัดการคลังและการตั้งค่า",
            languageLabels = mapOf(
                SharedAppLanguage.System to "ตามระบบ",
                SharedAppLanguage.English to "อังกฤษ",
                SharedAppLanguage.Thai to "ไทย",
                SharedAppLanguage.ChineseSimplified to "จีนตัวย่อ"
            )
        )

        private fun chinese() = english().copy(
            welcomeSubtitle = "跨平台桌面伴侣",
            runningOn = "运行于",
            languageSection = "语言",
            platformSupport = "平台支持",
            platformWindowsNote = "主要桌面目标，提供 MSI 安装包",
            platformLinuxNote = "DEB 包与 JVM 桌面运行时",
            platformIosNote = "共享 Kotlin 模块已支持 iOS 编译目标",
            platformAndroidNote = "完整播放、下载与音效功能",
            androidFullFeatures = "完整播放功能请在 Android 版使用。桌面版提供曲库管理与设置同步。",
            languageLabels = mapOf(
                SharedAppLanguage.System to "跟随系统",
                SharedAppLanguage.English to "英语",
                SharedAppLanguage.Thai to "泰语",
                SharedAppLanguage.ChineseSimplified to "简体中文"
            )
        )
    }
}
