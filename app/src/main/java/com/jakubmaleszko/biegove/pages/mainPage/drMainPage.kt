package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
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
    var currentStrokeBuilder by remember { mutableStateOf(Ink.Stroke.builder()) }
    val drawPath = remember { androidx.compose.ui.graphics.Path() }
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Note State
    var noteText by remember { mutableStateOf("") }

    val recognizer = remember { InkRecognizer() }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- DRAWING CANVAS ---
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
                        onDragEnd = {
                            strokes.add(currentStrokeBuilder.build())
                        }
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

        // --- TOP UI (NOTE INPUT) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // --- BOTTOM BUTTONS ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(), // Added to ensure horizontalAlignment works
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Correct name for Column
        ) {
            // Row for Clear and Proceed
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        drawPath.reset()
                        noteText = ""
                        motionTick++
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = {
                        // Logic: Allow saving if there's drawing OR a note
                        if (strokes.isEmpty() && noteText.isBlank()) return@Button

                        val noteToSave = noteText.ifBlank { null }

                        if (strokes.isNotEmpty()) {
                            val inkBuilder = Ink.builder()
                            strokes.forEach { inkBuilder.addStroke(it) }

                            recognizer.recognize(inkBuilder.build()) { digits ->
                                val runnerNum = digits.toIntOrNull()
                                if (runnerNum != null || noteToSave != null) {
                                    viewModel.addResultToSelectedRace(runnerNum, noteToSave)
                                    android.widget.Toast.makeText(context, "Added: ${digits.ifBlank { "Note" }}", android.widget.Toast.LENGTH_SHORT).show()
                                }

                                strokes.clear()
                                drawPath.reset()
                                noteText = ""
                                motionTick++
                            }
                        } else {
                            // Note only
                            viewModel.addResultToSelectedRace(null, noteToSave)
                            android.widget.Toast.makeText(context, "Added Note", android.widget.Toast.LENGTH_SHORT).show()
                            noteText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp).weight(1f)
                ) {
                    Text("Proceed")
                }
            }

            // Unknown Button
            OutlinedButton(
                onClick = {
                    viewModel.addResultToSelectedRace(999999, noteText.ifBlank { "Unknown" })
                    android.widget.Toast.makeText(context, "Added Unknown", android.widget.Toast.LENGTH_SHORT).show()
                    strokes.clear()
                    drawPath.reset()
                    noteText = ""
                    motionTick++
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text("Add Unknown (999999)")
            }
        }
    }
}