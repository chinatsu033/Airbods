package com.chinatsu.airbods.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chinatsu.airbods.R
import com.chinatsu.airbods.data.Prefs
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastLocationScreen(
    prefs: Prefs,
    hasLocation: Boolean,
    hasNotifications: Boolean,
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onClear: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRefresh: () -> Unit,
) {
    val has = prefs.hasLastLocation()
    // Include refreshKey in composition identity
    val _epoch = refreshKey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.last_location), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (has) {
                Card(
                    Modifier.fillMaxWidth().height(220.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    OsmMap(prefs.lastLat, prefs.lastLng, Modifier.fillMaxSize())
                }
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(prefs.lastLocationDeviceName ?: "AirPods", fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.coords, prefs.lastLat, prefs.lastLng),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        prefs.lastLocationAddress?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(prefs.lastLocationTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenMap(prefs.lastLat, prefs.lastLng) },
                                shape = MaterialTheme.shapes.medium,
                            ) { Text(stringResource(R.string.open_map)) }
                            OutlinedButton(onClick = onClear, shape = MaterialTheme.shapes.medium) {
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }
                }
            } else {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.last_location_empty_title), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.last_location_how), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.last_location_reqs), fontWeight = FontWeight.Medium)
                    Text((if (hasNotifications) "✓ " else "○ ") + stringResource(R.string.req_notif))
                    if (!hasNotifications) {
                        Button(onClick = onRequestNotifications, shape = MaterialTheme.shapes.medium) {
                            Text(stringResource(R.string.grant_notifications))
                        }
                    }
                    Text((if (hasLocation) "✓ " else "○ ") + stringResource(R.string.req_loc))
                    if (!hasLocation) {
                        Button(onClick = onRequestLocation, shape = MaterialTheme.shapes.medium) {
                            Text(stringResource(R.string.grant_location))
                        }
                    }
                    Text(stringResource(R.string.req_fgs), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onRefresh, shape = MaterialTheme.shapes.medium) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OsmMap(lat: Double, lng: Double, modifier: Modifier = Modifier) {
    val html = remember(lat, lng) {
        val d = 0.008
        """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width,initial-scale=1"/>
        <style>html,body,iframe{margin:0;width:100%;height:100%;border:0}</style>
        </head><body>
        <iframe src="https://www.openstreetmap.org/export/embed.html?bbox=${lng - d}%2C${lat - d}%2C${lng + d}%2C${lat + d}&amp;layer=mapnik&amp;marker=$lat%2C$lng"></iframe>
        </body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = modifier.clip(MaterialTheme.shapes.large),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
            }
        },
        update = { it.loadDataWithBaseURL("https://www.openstreetmap.org/", html, "text/html", "UTF-8", null) },
    )
}
