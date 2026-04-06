package com.interactiveword.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interactiveword.data.model.User
import com.interactiveword.ui.theme.BrandAmberLight
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@Composable
fun UserHeader(user: User, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border   = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(user.username, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text  = "Lv.${user.xp / 100 + 1} · ${user.rank}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkMutedText,
                    )
                }
                // 연속 학습 스트릭
                Surface(
                    shape = CircleShape,
                    color = BrandAmberLight.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "스트릭",
                            tint = BrandAmberLight,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text  = "0일",   // TODO: 스트릭 추가 시 교체
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandAmberLight,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // XP 바
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("XP", style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
                Text(
                    "${user.xp} / ${(user.xp / 500 + 1) * 500}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress  = { (user.xp % 500) / 500f },
                modifier  = Modifier.fillMaxWidth().height(6.dp),
                color     = BrandGreenLight,
                trackColor = DarkOutline,
            )

            Spacer(Modifier.height(8.dp))

            // 단어 슬롯
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("단어 슬롯", style = MaterialTheme.typography.bodySmall, color = DarkMutedText)
                Text(
                    "0 / ${user.maxWordSlots}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkMutedText,
                )
            }
        }
    }
}
