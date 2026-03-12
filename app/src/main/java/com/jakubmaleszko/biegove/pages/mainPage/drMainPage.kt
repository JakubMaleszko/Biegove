package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.jakubmaleszko.biegove.BiegoveViewModel


@Composable
fun DrMainPage(
    viewModel: BiegoveViewModel
) {
    val context = LocalContext.current

    // Drawing State
    var motionTick by remember { mutableIntStateOf(0) }
    val strokes = remember { mutableStateListOf<Ink.Stroke>() }
    var currentStrokeBuilder by remember { mutableStateOf<Ink.Stroke.Builder>(Ink.Stroke.builder()) }
    val drawPath = remember { androidx.compose.ui.graphics.Path() }
    val onSurface = MaterialTheme.colorScheme.onSurface

    val recognizer = remember { InkRecognizer(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStrokeBuilder = Ink.Stroke.builder()
                            currentStrokeBuilder.addPoint(Ink.Point.create(offset.x, offset.y))
                            drawPath.moveTo(offset.x, offset.y)
                        },
                        onDrag = { change, _ ->
                            val p = change.position
                            currentStrokeBuilder.addPoint(Ink.Point.create(p.x, p.y))
                            drawPath.lineTo(p.x, p.y)
                            motionTick++
                        },
                        onDragEnd = { strokes.add(currentStrokeBuilder.build()) }
                    )
                }
        ) {
            motionTick.let {
                drawPath(
                    path = drawPath,
                    color = onSurface,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 12f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }

        // --- BUTTONS ONLY ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedButton(
                onClick = {
                    strokes.clear()
                    drawPath.reset()
                    motionTick++
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear")
            }

            Button(
                onClick = {
                    if (strokes.isEmpty()) return@Button
                    val inkBuilder = Ink.builder()
                    strokes.forEach { inkBuilder.addStroke(it) }

                    recognizer.recognize(inkBuilder.build()) { digits ->
                        val msg = if (digits.isNotEmpty()) {
                            viewModel.addTimestamp(digits.toInt())
                            "Added $digits"
                        } else {
                            "Not recognized"
                        }

                        // Native Android Info Box (Toast)
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

                        strokes.clear()
                        drawPath.reset()
                        motionTick++
                    }
                }
            ) {
                Text("Proceed")
            }
        }
    }
}