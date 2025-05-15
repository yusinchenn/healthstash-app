package com.example.healthstash.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthstash.data.model.Medication

@Composable
fun MedicationCard(
    medication: Medication,
    onEditClick: (Medication) -> Unit,
    onTakenClick: (Medication, Int) -> Unit,
    onDeleteClick: (Medication) -> Unit
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
            // ✅ 使用共用圖示元件
            MedicationIcon(
                iconStr = medication.iconResId,
                contentDescription = medication.name,
                size = 60.dp,
                modifier = Modifier.clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "服用時間: ${medication.usageTimes.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        medication.remainingQuantity.toFloat() / medication.totalQuantity.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    text = "剩餘量: ${medication.remainingQuantity} / ${medication.totalQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

            IconButton(onClick = { onEditClick(medication) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "變更")
            }
        }
    }
}

@Composable
fun MedicationIcon(
    iconStr: String?,
    contentDescription: String? = null,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = runCatching { Uri.parse(iconStr) }.getOrNull()

    when {
        parsedUri?.scheme == "content" || parsedUri?.scheme == "file" -> {
            AsyncImage(
                model = parsedUri,
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                contentScale = ContentScale.Crop
            )
        }
        !iconStr.isNullOrBlank() -> {
            val resId = context.resources.getIdentifier(iconStr, "drawable", context.packageName)
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    modifier = modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = "預設圖示",
                    modifier = modifier.size(size)
                )
            }
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = "預設圖示",
                modifier = modifier.size(size)
            )
        }
    }
}
