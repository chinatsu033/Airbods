package com.chinatsu.airbods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chinatsu.airbods.R
import com.chinatsu.airbods.bluetooth.AdaptiveLevel
import com.chinatsu.airbods.bluetooth.AirPodsModel
import com.chinatsu.airbods.bluetooth.NoiseMode

@Composable
fun ModeSwitcher(
    model: AirPodsModel,
    current: NoiseMode?,
    adaptiveLevel: Int,
    enabled: Boolean,
    onMode: (NoiseMode) -> Unit,
    onAdaptiveLevel: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.section_modes), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (model.hasAnc || model == AirPodsModel.UNKNOWN) {
                ModeBtn(
                    label = stringResource(R.string.mode_anc),
                    selected = current == NoiseMode.ANC,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onMode(NoiseMode.ANC) },
                )
            }
            ModeBtn(
                label = stringResource(R.string.mode_transparency),
                selected = current == NoiseMode.TRANSPARENCY,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onMode(NoiseMode.TRANSPARENCY) },
            )
            if (model.showAdaptive()) {
                ModeBtn(
                    label = stringResource(R.string.mode_adaptive),
                    selected = current == NoiseMode.ADAPTIVE,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onMode(NoiseMode.ADAPTIVE) },
                )
            }
        }

        if (model.showAdaptive() && (current == NoiseMode.ADAPTIVE || model.hasAdaptiveStrength)) {
            Text(stringResource(R.string.adaptive_strength), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdaptiveLevel.entries.forEach { preset ->
                    val selected = current == NoiseMode.ADAPTIVE &&
                        AdaptiveLevel.nearest(adaptiveLevel) == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onAdaptiveLevel(preset.level) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                when (preset) {
                                    AdaptiveLevel.MORE_TRANSPARENCY -> stringResource(R.string.adaptive_0)
                                    AdaptiveLevel.BALANCED -> stringResource(R.string.adaptive_50)
                                    AdaptiveLevel.MORE_ANC -> stringResource(R.string.adaptive_100)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeBtn(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) { Text(label) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) { Text(label) }
    }
}
