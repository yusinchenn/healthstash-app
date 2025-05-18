package com.example.healthstash.ui.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.healthstash.data.model.Medication


// 顯示單一藥品資訊的卡片 UI 元件
@Composable
fun MedicationCard(
    medication: Medication,
    onEditClick: (Medication) -> Unit,        // 點擊編輯按鈕時觸發
    onTakenClick: (Medication, Int) -> Unit,  // 點擊服藥按鈕時觸發
    onDeleteClick: (Medication) -> Unit       // 點擊刪除按鈕時觸發（當數量為 0）
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MedicationDisplayIcon(
                iconUriString = medication.iconUriString,
                iconDrawableResId = medication.iconDrawableResId,
                contentDescription = medication.name,
                size = 60.dp,
                modifier = Modifier.clip(CircleShape) // clip 應該在 MedicationDisplayIcon 內部處理或傳遞給它
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 顯示藥品名稱
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 顯示服用時間清單
                Text(
                    text = "服用時間: ${medication.usageTimes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 用量進度條
                LinearProgressIndicator(
                    progress = {
                        medication.remainingQuantity.toFloat() / medication.totalQuantity.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )

                // 顯示剩餘量
                Text(
                    text = "剩餘量: ${medication.remainingQuantity} / ${medication.totalQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 依剩餘數量顯示「服用」或「刪除」按鈕
            if (medication.remainingQuantity > 0) {
                IconButton(onClick = {
                    onTakenClick(medication, 1)
                }) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "我已服用",
                        tint = Color.Green
                    )
                }
            } else {
                IconButton(onClick = {
                    onDeleteClick(medication)
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "刪除此藥品",
                        tint = Color.Red
                    )
                }
            }

            // 編輯按鈕
            IconButton(onClick = { onEditClick(medication) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "變更")
            }
        }
    }
}
@SuppressLint("DiscouragedApi")
@Composable
fun MedicationDisplayIcon( // 重新命名以避免與 Material Icon 衝突，或保持原名並注意導入
    iconUriString: String?,
    @DrawableRes iconDrawableResId: Int?,
    contentDescription: String?,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    when {
        !iconUriString.isNullOrBlank() -> { // 優先使用 URI
            AsyncImage(
                model = iconUriString.toUri(), // 解析 URI 字串
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Crop,
                error = painterResource(id = com.example.healthstash.R.drawable.ic_default_med) // 添加錯誤時的佔位圖
            )
        }
        iconDrawableResId != null && iconDrawableResId != 0 -> { // 其次使用 Drawable 資源 ID
            Image(
                painter = painterResource(id = iconDrawableResId),
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Crop
            )
        }
        else -> { // 如果都沒有，顯示通用預設圖示
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = contentDescription ?: "預設藥品圖示",
                modifier = modifier.size(size),
                // tint = MaterialTheme.colorScheme.primary // 可以考慮添加 tint
            )
        }
    }
}
