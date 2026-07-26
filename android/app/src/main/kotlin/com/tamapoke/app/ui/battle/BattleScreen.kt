package com.tamapoke.app.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tamapoke.app.R
import com.tamapoke.app.sprite.AnimatedSprite
import com.tamapoke.app.ui.device.RoundDeviceFrame
import com.tamapoke.core.battle.BattleAction
import com.tamapoke.core.dex.DexTable
import com.tamapoke.core.enums.BattleRewardStat

/**
 * Manual wild-encounter battle, ported from the ShadowEnemy expanded fork's
 * "combate salvaje manual" (TamaPoke.ino): Attack/Quick/Heavy/Dodge/Rest
 * turn actions against a randomly rolled wild Pokemon, then an optional
 * catch (or, on a close loss, a gentler "respect catch") attempt.
 */
@Composable
fun BattleScreen(
    battle: BattleUiState,
    dex: DexTable,
    playerSpeciesId: Int,
    playerShiny: Boolean,
    onAct: (BattleAction) -> Unit,
    onCatch: () -> Unit,
    onClose: () -> Unit,
) {
    val wildEntry = dex[battle.wildDexId]
    val runtime = battle.runtime

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight) * 0.94f
        RoundDeviceFrame(Modifier.size(diameter)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF10131A))) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Column(Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            stringResource(R.string.battle_wild_appeared, wildEntry.name, runtime.enemy.level),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        CombatantRow(
                            name = wildEntry.name,
                            speciesId = battle.wildDexId,
                            shiny = false,
                            hp = runtime.enemyHp,
                            maxHp = runtime.enemyMaxHp,
                        )
                        Row(Modifier.padding(vertical = 8.dp)) {
                            Text("VS", style = MaterialTheme.typography.labelLarge)
                        }
                        CombatantRow(
                            name = stringResource(R.string.battle_you),
                            speciesId = playerSpeciesId,
                            shiny = playerShiny,
                            hp = runtime.playerHp,
                            maxHp = runtime.playerMaxHp,
                        )

                        battle.lastTurn?.let { turn ->
                            Text(
                                narrate(turn),
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        if (!battle.ended) {
                            Row(Modifier.padding(top = 12.dp)) {
                                Button(onClick = { onAct(BattleAction.ATTACK) }, modifier = Modifier.padding(end = 6.dp)) {
                                    Text(stringResource(R.string.battle_attack))
                                }
                                Button(onClick = { onAct(BattleAction.ATTACK_QUICK) }, modifier = Modifier.padding(end = 6.dp)) {
                                    Text(stringResource(R.string.battle_quick))
                                }
                                Button(onClick = { onAct(BattleAction.ATTACK_HEAVY) }) { Text(stringResource(R.string.battle_heavy)) }
                            }
                            Row(Modifier.padding(top = 6.dp)) {
                                Button(onClick = { onAct(BattleAction.DODGE) }, modifier = Modifier.padding(end = 6.dp)) {
                                    Text(stringResource(R.string.battle_dodge))
                                }
                                Button(
                                    onClick = { onAct(BattleAction.REST) },
                                    enabled = runtime.restUsesLeft > 0,
                                ) { Text(stringResource(R.string.battle_rest_fmt, runtime.restUsesLeft)) }
                            }
                        } else {
                            Column(Modifier.padding(top = 12.dp)) {
                                Text(
                                    stringResource(if (battle.playerWon) R.string.battle_win else R.string.battle_lose),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (battle.playerWon) Color(0xFF6EDC7A) else Color(0xFFE5726A),
                                )
                                battle.reward?.let { reward ->
                                    if (reward.stat != BattleRewardStat.NONE) {
                                        Text(stringResource(R.string.battle_reward_fmt, reward.stat.name, reward.amount))
                                    }
                                }
                                when (battle.catchResult) {
                                    true -> Text(stringResource(R.string.battle_caught), color = Color(0xFFFFD54A))
                                    false -> Text(stringResource(R.string.battle_escaped))
                                    null -> if (battle.catchOffered) {
                                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Button(onClick = onCatch, modifier = Modifier.padding(end = 8.dp)) {
                                                Text(
                                                    stringResource(
                                                        if (battle.catchIsRespect) R.string.battle_respect_catch else R.string.battle_catch,
                                                    ),
                                                )
                                            }
                                            Text(stringResource(R.string.battle_catch_chance_fmt, battle.catchChancePct))
                                        }
                                    }
                                }
                                Button(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) {
                                    Text(stringResource(R.string.battle_close))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatantRow(name: String, speciesId: Int, shiny: Boolean, hp: Int, maxHp: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedSprite(
            speciesId = speciesId,
            action = "idle",
            shiny = shiny,
            modifier = Modifier.size(56.dp),
            placeholder = { Box(Modifier.size(56.dp).background(Color(0xFF2A2E38))) },
        )
        Column(Modifier.padding(start = 10.dp).fillMaxWidth()) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            HpBar(hp, maxHp)
        }
    }
}

@Composable
private fun HpBar(hp: Int, maxHp: Int) {
    val fraction = if (maxHp > 0) (hp.toFloat() / maxHp).coerceIn(0f, 1f) else 0f
    val color = when {
        fraction > 0.5f -> Color(0xFF6EDC7A)
        fraction > 0.2f -> Color(0xFFF2C744)
        else -> Color(0xFFE5726A)
    }
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2A2E38))) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color),
        )
    }
    Text("$hp/$maxHp", style = MaterialTheme.typography.labelSmall)
}

private fun narrate(turn: com.tamapoke.core.battle.BattleTurnResult): String = buildString {
    if (turn.playerRested) {
        append(if (turn.restFailed) "No rests left! " else "Rested +${turn.playerHeal} HP. ")
    } else if (turn.enemyDodged) {
        append("The wild Pokemon dodged! ")
    } else if (turn.playerDamage > 0) {
        append("Hit for ${turn.playerDamage}")
        if (turn.counterUsed) append(" (counter!)")
        append(". ")
    }
    if (turn.playerDodged) append("You dodged and readied a counter! ")
    else if (turn.enemyDamage > 0) append("Took ${turn.enemyDamage} damage. ")
}
