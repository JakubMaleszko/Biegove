package com.jakubmaleszko.biegove.pages.mainPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.jakubmaleszko.biegove.BiegoveViewModel
import kotlinx.coroutines.launch


@Composable
fun DrMainPage(
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    viewModel: BiegoveViewModel
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<Ink.Stroke>() }
    var currentStroke by remember { mutableStateOf(Ink.Stroke.builder()) }
    val recognizer = remember { InkRecognizer(context) }

    suspend fun saveTimestamp() {
        val num = number.toIntOrNull() ?: return
        viewModel.addTimestamp(num)
        snackbarHostState.showSnackbar("Added entry with number: $num")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = Ink.Stroke.builder()
                            currentStroke.addPoint(
                                Ink.Point.create(offset.x, offset.y, System.currentTimeMillis())
                            )
                        },
                        onDrag = { change, _ ->
                            val p = change.position
                            currentStroke.addPoint(
                                Ink.Point.create(p.x, p.y, System.currentTimeMillis())
                            )
                        },
                        onDragEnd = {
                            strokes.add(currentStroke.build())
                        }
                    )
                }
        ) {
            strokes.forEach { stroke ->
                val points = stroke.points
                for (i in 1 until points.size) {
                    drawLine(
                        Color.Black,
                        start = Offset(points[i - 1].x, points[i - 1].y),
                        end = Offset(points[i].x, points[i].y),
                        strokeWidth = 8f
                    )
                }
            }
        }

        // Recognize button
        Button(onClick = {
            val inkBuilder = Ink.builder()
            strokes.forEach { inkBuilder.addStroke(it) }

            recognizer.recognize(inkBuilder.build()) { digits ->
                if (digits.isNotEmpty()) {
                    number = digits
                    scope.launch { saveTimestamp() }
                    strokes.clear()
                } else {
                    android.util.Log.d("DrMainPage", "No digits recognized yet")
                    // keep strokes so user can try again
                }
            }

        }) {
            Text("Recognize")
        }

        // Optional: show recognized number
        if (number.isNotEmpty()) {
            Text("Recognized number: $number")
        }
    }
}