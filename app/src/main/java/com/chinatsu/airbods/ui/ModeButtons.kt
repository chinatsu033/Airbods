package com.chinatsu.airbods.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chinatsu.airbods.R
import com.chinatsu.airbods.bluetooth.AdaptiveLevel
import com.chinatsu.airbods.bluetooth.AirPodsModel
import com.chinatsu.airbods.bluetooth.NoiseMode

/**
 * iOS Control Center–style: icon-only mode / strength toggles.
 * Labels live only in contentDescription (localized).
 */
@Composable
fun ModeSwitcher(
    model: AirPodsModel,
    current: NoiseMode?,
    adaptiveLevel: Int,
    enabled: Boolean,
    onMode: (NoiseMode) -> Unit,
    onAdaptiveLevel: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.section_modes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Order matches iOS Control Center: Transparency · Adaptive · Noise Cancellation
            ModeIconButton(
                selected = current == NoiseMode.TRANSPARENCY,
                enabled = enabled,
                iconRes = R.drawable.ic_mode_transparency,
                contentDescription = stringResource(R.string.mode_transparency),
                onClick = { onMode(NoiseMode.TRANSPARENCY) },
            )
            if (model.showAdaptive()) {
                ModeIconButton(
                    selected = current == NoiseMode.ADAPTIVE,
                    enabled = enabled,
                    iconRes = R.drawable.ic_mode_adaptive,
                    contentDescription = stringResource(R.string.mode_adaptive),
                    onClick = { onMode(NoiseMode.ADAPTIVE) },
                )
            }
            if (model.hasAnc || model == AirPodsModel.UNKNOWN) {
                ModeIconButton(
                    selected = current == NoiseMode.ANC,
                    enabled = enabled,
                    iconRes = R.drawable.ic_mode_anc,
                    contentDescription = stringResource(R.string.mode_anc),
                    onClick = { onMode(NoiseMode.ANC) },
                )
            }
        }

        if (model.showAdaptive() && (current == NoiseMode.ADAPTIVE || model.hasAdaptiveStrength)) {
            Text(
                stringResource(R.string.adaptive_strength),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrengthIconButton(
                    selected = current == NoiseMode.ADAPTIVE &&
                        AdaptiveLevel.nearest(adaptiveLevel) == AdaptiveLevel.MORE_TRANSPARENCY,
                    enabled = enabled,
                    iconRes = R.drawable.ic_strength_light,
                    contentDescription = stringResource(R.string.adaptive_0),
                    onClick = { onAdaptiveLevel(AdaptiveLevel.MORE_TRANSPARENCY.level) },
                )
                StrengthIconButton(
                    selected = current == NoiseMode.ADAPTIVE &&
                        AdaptiveLevel.nearest(adaptiveLevel) == AdaptiveLevel.BALANCED,
                    enabled = enabled,
                    iconRes = R.drawable.ic_strength_standard,
                    contentDescription = stringResource(R.string.adaptive_50),
                    onClick = { onAdaptiveLevel(AdaptiveLevel.BALANCED.level) },
                )
                StrengthIconButton(
                    selected = current == NoiseMode.ADAPTIVE &&
                        AdaptiveLevel.nearest(adaptiveLevel) == AdaptiveLevel.MORE_ANC,
                    enabled = enabled,
                    iconRes = R.drawable.ic_strength_heavy,
                    contentDescription = stringResource(R.string.adaptive_100),
                    onClick = { onAdaptiveLevel(AdaptiveLevel.MORE_ANC.level) },
                )
            }
        }
    }
}

@Composable
private fun ModeIconButton(
    selected: Boolean,
    enabled: Boolean,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = IconButtonDefaults.filledIconToggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    )
    FilledIconToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        enabled = enabled,
        modifier = Modifier.size(64.dp),
        shape = MaterialTheme.shapes.medium,
        colors = colors,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun StrengthIconButton(
    selected: Boolean,
    enabled: Boolean,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    if (selected) {
        FilledIconToggleButton(
            checked = true,
            onCheckedChange = { onClick() },
            enabled = enabled,
            modifier = Modifier.size(56.dp).height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    } else {
        OutlinedIconToggleButton(
            checked = false,
            onCheckedChange = { onClick() },
            enabled = enabled,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
