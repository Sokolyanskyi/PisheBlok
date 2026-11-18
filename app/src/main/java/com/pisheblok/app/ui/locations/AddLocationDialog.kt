package com.pisheblok.app.ui.locations

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pisheblok.app.R
import com.pisheblok.app.ui.components.EmojiPicker
import com.pisheblok.app.ui.theme.Blok

/**
 * Add / rename a room, floor, storage place or section.
 * Pass [initialName]/[initialIcon] for edit mode.
 */
@Composable
fun AddLocationDialog(
    titleRes: Int,
    iconSet: List<String>,
    initialName: String?,
    initialIcon: String?,
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName ?: "") }
    var icon by rememberSaveable { mutableStateOf(initialIcon ?: iconSet.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            // Пикер сам скроллится внутри группы — общий скролл диалога
            // только мешал бы ему и тянул кнопки.
            Column(
                verticalArrangement = Arrangement.spacedBy(Blok.Space.m),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.location_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_name_field"),
                    shape = MaterialTheme.shapes.small,
                )
                Text(
                    stringResource(R.string.choose_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EmojiPicker(emojis = iconSet, selected = icon, onSelect = { icon = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, icon) },
            ) {
                Text(stringResource(if (initialName == null) R.string.add else R.string.save))
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                if (onDelete != null) {
                    // Удаление локации необратимо — hold-to-confirm прямо в диалоге.
                    com.pisheblok.app.ui.motion.components.HoldButton(
                        label = stringResource(R.string.delete),
                        onConfirm = onDelete,
                        height = 40.dp,
                        modifier = Modifier.width(132.dp),
                    )
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
