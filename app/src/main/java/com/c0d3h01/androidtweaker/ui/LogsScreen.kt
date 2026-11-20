package com.c0d3h01.androidtweaker.ui

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.ActivityNotifier
import com.c0d3h01.androidtweaker.data.LogLevel
import com.c0d3h01.androidtweaker.data.LogReader
import com.c0d3h01.androidtweaker.data.LogResult
import com.c0d3h01.androidtweaker.data.LogSource
import com.c0d3h01.androidtweaker.data.RootInfo
import com.c0d3h01.androidtweaker.data.classifyLogLine
import com.c0d3h01.androidtweaker.data.path
import com.c0d3h01.androidtweaker.util.RootShell
import com.c0d3h01.androidtweaker.util.clockWithSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogsScreen(
  root: RootInfo?,
  logPath: String,
  onRetryRoot: () -> Unit,
) {
  var sourceName by rememberSaveable { mutableStateOf(LogSource.TWEAKS.name) }
  val source = LogSource.entries.firstOrNull { it.name == sourceName } ?: LogSource.TWEAKS
  val sourceLabels = LogSource.entries.map { it.title }
  val sourceFiles =
    LogSource.entries.map {
      when (it) {
        LogSource.TWEAKS -> "tweaks.log"
        LogSource.DMESG -> "dmesg"
        LogSource.LOGCAT -> "logcat"
      }
    }
  var query by rememberSaveable { mutableStateOf("") }
  var autoRefresh by rememberSaveable { mutableStateOf(true) }
  var lines by remember { mutableStateOf<List<String>>(emptyList()) }
  var missing by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }
  var updatedAt by remember { mutableLongStateOf(0L) }
  var followTail by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val listState = rememberLazyListState()

  suspend fun load() {
    if (root?.granted != true) return
    loading = true
    val result =
      withContext(Dispatchers.IO) {
        try {
          val runner = RootShell.ensure()
          LogReader.read(source, runner, logPath)
        } catch (e: Exception) {
          LogResult(source, emptyList(), error = e.message ?: "Shell unavailable")
        }
      }
    val wasAtTail = !listState.canScrollForward
    lines = result.lines
    missing = result.missing
    error = result.error
    updatedAt = System.currentTimeMillis()
    loading = false
    followTail = wasAtTail
  }
  LaunchedEffect(source, autoRefresh, root?.granted) {
    if (root?.granted != true) return@LaunchedEffect
    load()
    if (!autoRefresh) return@LaunchedEffect
    while (true) {
      delay(2500)
      load()
    }
  }

  LaunchedEffect(query) {
    listState.scrollToItem(0)
  }
  val filtered =
    remember(lines, query) {
      if (query.isBlank()) lines else lines.filter { it.contains(query, ignoreCase = true) }
    }
  LaunchedEffect(filtered.size, autoRefresh) {
    if (autoRefresh && followTail && filtered.isNotEmpty()) {
      listState.animateScrollToItem(filtered.lastIndex)
    }
  }

  fun share() {
    val body = filtered.joinToString("\n").take(60_000)
    if (body.isBlank()) return
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "AndroidTweaker ${source.title} log")
        putExtra(Intent.EXTRA_TEXT, body)
      }
    context.startActivity(Intent.createChooser(intent, "Share log"))
    ActivityNotifier.logExported(context, source, body.lines().size)
  }

  fun copy(line: String) {
    clipboard.setText(AnnotatedString(line))
    Toast.makeText(context, "Line copied", Toast.LENGTH_SHORT).show()
  }

  Column(
    Modifier.fillMaxSize().padding(horizontal = Bench.Gutter).padding(bottom = Bench.BarClearance),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Spacer(Modifier.height(0.dp))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
      Wordmark(onRefresh = { scope.launch { load() } })
    }
    ScreenTitle(title = "Logs", subtitle = "Engine & kernel output") {
      if (root?.granted == true) {
        StatusChip("Rooted", Bench.Mint)
      } else if (root != null) {
        StatusChip("No root", Bench.Rose)
      }
    }

    if (root?.granted == false) {
      EmptyState(
        title = "Root required",
        body = "Logs are read through the root shell. Grant superuser access in your root manager, then retry.",
        icon = Icons.Outlined.Terminal,
        action = { GhostButton("Retry root", onClick = onRetryRoot) },
      )
      return@Column
    }

    BenchTabStrip(
      labels = sourceLabels,
      secondaryLabels = sourceFiles,
      selected = source.ordinal,
      onSelect = {
        sourceName = LogSource.entries[it].name
        query = ""
      },
    )

    BenchSearchField(query, { query = it }, "Filter lines, tags, keywords…")

    Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      listOf("throttle", "gpu", "error", "root", "su").forEach { chip ->
        val active = query.equals(chip, ignoreCase = true)
        Box(
          Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) Bench.SignalSoft else Bench.Sunken)
            .border(1.dp, if (active) Bench.Signal.copy(alpha = 0.45f) else Bench.Hairline, RoundedCornerShape(5.dp))
            .clickable(role = Role.Button) { query = if (active) "" else chip }
            .padding(horizontal = 8.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(chip.uppercase(), style = AppType.micro, color = if (active) Bench.Signal else Bench.InkMuted)
        }
      }
      val allActive = query.isBlank()
      Box(
        Modifier
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(if (allActive) Bench.SignalSoft else Bench.Sunken)
          .border(1.dp, if (allActive) Bench.Signal.copy(alpha = 0.45f) else Bench.Hairline, RoundedCornerShape(5.dp))
          .clickable(role = Role.Button) { query = "" }
          .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text("ALL", style = AppType.micro, color = if (allActive) Bench.Signal else Bench.InkFaint)
      }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      StatusChip("${filtered.size} lines", Bench.InkMuted, dot = false)
      Spacer(Modifier.weight(1f))
      Text("AUTO", style = AppType.micro, color = Bench.InkFaint)
      Spacer(Modifier.width(2.dp))
      Switch(
        checked = autoRefresh,
        onCheckedChange = { autoRefresh = it },
        modifier = Modifier.semantics { contentDescription = "Auto-refresh" },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = Bench.Canvas,
            checkedTrackColor = Bench.Signal,
            uncheckedThumbColor = Bench.InkMuted,
            uncheckedTrackColor = Bench.PanelRaised,
            uncheckedBorderColor = Bench.HairlineStrong,
          ),
      )
      Spacer(Modifier.width(2.dp))
      IconSquare(
        Icons.Outlined.Share,
        "Share log",
        onClick = { share() },
        enabled = filtered.isNotEmpty(),
      )
      Spacer(Modifier.width(8.dp))
      IconSquare(Icons.Outlined.Refresh, "Refresh", onClick = { scope.launch { load() } })
    }

    Panel(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentPadding = PaddingValues(0.dp),
      container = Bench.Sunken,
    ) {
      when {
        root == null -> {
          LogEmpty("Checking root…", "Waiting for the root shell before reading ${source.title.lowercase()} output.", Bench.InkMuted)
        }

        error != null -> {
          LogEmpty("Log unavailable", error ?: "Shell error", Bench.Rose)
        }

        missing -> {
          LogEmpty("No log yet", "Apply a profile to start writing the on-device tweak log.", Bench.Amber)
        }

        filtered.isEmpty() && loading -> {
          LogEmpty("Reading…", "Pulling the latest ${source.title.lowercase()} buffer.", Bench.InkMuted)
        }

        filtered.isEmpty() -> {
          LogEmpty("No matching lines", "Clear the filter or switch source.", Bench.InkMuted)
        }

        else -> {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            itemsIndexed(filtered) { index, line ->
              LogLineRow(index, line, onClick = { copy(line) })
            }
          }
        }
      }
    }

    Row(
      Modifier.fillMaxWidth().padding(bottom = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "${source.path(logPath)}  ·  ${lines.size} LINES",
        modifier = Modifier.weight(1f),
        style = AppType.micro,
        color = Bench.InkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.width(10.dp))
      Text(
        if (updatedAt > 0) "UPDATED ${clockWithSeconds(context, updatedAt)}" else "—",
        style = AppType.micro,
        color = Bench.InkFaint,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun LogEmpty(
  title: String,
  body: String,
  tone: Color,
) {
  Column(
    Modifier.fillMaxSize().padding(18.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = tone)
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
  }
}

@Composable
private fun LogLineRow(
  index: Int,
  line: String,
  onClick: () -> Unit,
) {
  val level = classifyLogLine(line)
  val tone =
    when (level) {
      LogLevel.ERROR -> Bench.Rose
      LogLevel.WARN -> Bench.Amber
      LogLevel.OK -> Bench.Mint
      LogLevel.INFO -> Bench.InkMuted
    }
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(4.dp))
      .clickable(role = Role.Button, onClickLabel = "Copy line", onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 3.dp),
  ) {
    Text(
      (index + 1).toString().padStart(3, '0'),
      style = AppType.log.copy(letterSpacing = 0.sp),
      color = Bench.InkFaint.copy(alpha = 0.7f),
      modifier = Modifier.width(28.dp),
    )
    Box(
      Modifier
        .padding(top = 2.dp)
        .width(2.dp)
        .height(11.dp)
        .background(tone, RoundedCornerShape(1.dp)),
    )
    Spacer(Modifier.width(7.dp))
    Text(
      line,
      style = AppType.log,
      color = tone,
      modifier = Modifier.weight(1f),
    )
    Icon(
      Icons.Outlined.ContentCopy,
      contentDescription = null,
      tint = Bench.InkFaint.copy(alpha = 0.55f),
      modifier = Modifier.padding(start = 6.dp).size(13.dp),
    )
  }
}
