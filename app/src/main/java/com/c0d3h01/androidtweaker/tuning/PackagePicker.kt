package com.c0d3h01.androidtweaker.tuning

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.components.EmptyState
import com.c0d3h01.androidtweaker.ui.components.LoadingLine
import com.c0d3h01.androidtweaker.ui.components.SearchField
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which package list an [AppPickerDialog] is editing. */
enum class AppPickerKind(
  val title: String,
  val blurb: String,
) {
  HEAVY("Heavy apps", "Performance game mode and RT priority while they run"),
  LIGHT("Light apps", "Battery game mode for apps that do not need speed"),
  WHITELIST("Doze whitelist", "Apps that must keep running during forced idle"),
  BLOAT("Bloatware", "Uninstall for user 0; every removal can be restored"),
}

data class AppEntry(
  val label: String,
  val pkg: String,
)

@Composable
internal fun PickerRow(
  label: String,
  value: String,
  onClick: () -> Unit,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
    Spacer(Modifier.weight(1f))
    Text(value, style = AppType.micro, color = AppColors.InkMuted)
    Spacer(Modifier.width(6.dp))
    Text("CHANGE", style = AppType.micro, color = AppColors.Signal)
  }
}

/** Searchable multi-select over launchable apps plus anything already selected. */
@Composable
fun AppPickerDialog(
  kind: AppPickerKind,
  selected: Set<String>,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
  var query by remember { mutableStateOf("") }
  var picked by remember { mutableStateOf(selected) }
  var loading by remember { mutableStateOf(true) }

  LaunchedEffect(kind) {
    apps = withContext(Dispatchers.IO) { loadApps(context, selected) }
    loading = false
  }

  val shown =
    apps.filter {
      query.isBlank() ||
        it.label.contains(query, ignoreCase = true) ||
        it.pkg.contains(query, ignoreCase = true)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = AppColors.Panel,
    titleContentColor = AppColors.Ink,
    textContentColor = AppColors.InkMuted,
    shape = RoundedCornerShape(18.dp),
    title = {
      Column {
        Text(kind.title, style = MaterialTheme.typography.headlineSmall)
        Text(kind.blurb, style = MaterialTheme.typography.bodySmall, color = AppColors.InkFaint)
      }
    },
    text = {
      Column {
        SearchField(value = query, onValueChange = { query = it }, placeholder = "Search apps… e.g. YouTube")
        Spacer(Modifier.height(10.dp))
        if (loading) {
          LoadingLine()
        } else {
          LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(shown, key = { it.pkg }) { app ->
              val checked = app.pkg in picked
              Row(
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(9.dp))
                  .toggleable(
                    value = checked,
                    role = androidx.compose.ui.semantics.Role.Checkbox,
                    onValueChange = {
                      picked = if (checked) picked - app.pkg else picked + app.pkg
                    },
                  ).padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = null,
                  colors =
                    CheckboxDefaults.colors(
                      checkedColor = AppColors.Signal,
                      uncheckedColor = AppColors.HairlineStrong,
                      checkmarkColor = AppColors.Canvas,
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                  Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(app.pkg, style = AppType.mono, color = AppColors.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
            if (shown.isEmpty()) {
              item { EmptyState("No apps match", "Try another name or package id.") }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(picked) }) {
        Text("SAVE", style = MaterialTheme.typography.labelLarge, color = AppColors.Signal)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = AppColors.InkMuted)
      }
    },
  )
}

/** Launchable apps, plus any selected package the launcher does not list. */
private fun loadApps(
  context: Context,
  selected: Set<String>,
): List<AppEntry> {
  val pm = context.packageManager
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val entries =
    pm
      .queryIntentActivities(intent, 0)
      .mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        AppEntry(info.loadLabel(pm).toString(), pkg)
      }.distinctBy { it.pkg }
      .toMutableList()
  val known = entries.map { it.pkg }.toSet()
  selected.filter { it !in known }.sorted().forEach { pkg ->
    val label =
      runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString()
      }.getOrDefault(pkg)
    entries += AppEntry(label, pkg)
  }
  return entries.sortedBy { it.label.lowercase() }
}
