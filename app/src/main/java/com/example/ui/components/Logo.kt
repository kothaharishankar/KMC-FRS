package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun KakinadaSmartCityComposeLogo(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp).fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.width(180.dp).height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Wave
                val wavePath = Path().apply {
                    moveTo(0f, height * 0.8f)
                    cubicTo(
                        width * 0.3f, height * 0.6f,
                        width * 0.7f, height * 1.0f,
                        width, height * 0.8f
                    )
                    lineTo(width, height * 0.9f)
                    cubicTo(
                        width * 0.7f, height * 1.1f,
                        width * 0.3f, height * 0.7f,
                        0f, height * 0.9f
                    )
                    close()
                }
                drawPath(wavePath, color = Color(0xFF005A9C), style = Fill)
                
                // Second wave curve (lighter blue)
                val wave2Path = Path().apply {
                    moveTo(width * 0.2f, height * 0.95f)
                    cubicTo(
                        width * 0.5f, height * 0.85f,
                        width * 0.8f, height * 1.05f,
                        width * 0.95f, height * 0.6f
                    )
                }
                drawPath(wave2Path, color = Color(0xFF00B2D6), style = Stroke(width = 6f))

                // Lighthouse
                val lhLeft = width * 0.4f
                val lhRight = width * 0.55f
                val lhWidth = lhRight - lhLeft
                val lhBottom = height * 0.75f
                
                // Lighthouse base
                drawRect(
                    color = Color(0xFFF04B32),
                    topLeft = Offset(lhLeft, height * 0.25f),
                    size = Size(lhWidth, lhBottom - height * 0.25f)
                )
                // White stripes
                drawRect(
                    color = Color.White,
                    topLeft = Offset(lhLeft, height * 0.35f),
                    size = Size(lhWidth, height * 0.1f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(lhLeft, height * 0.55f),
                    size = Size(lhWidth, height * 0.1f)
                )

                // Lighthouse top
                drawRect(
                    color = Color(0xFFF04B32),
                    topLeft = Offset(lhLeft + lhWidth*0.2f, height * 0.15f),
                    size = Size(lhWidth*0.6f, height * 0.1f)
                )
                drawCircle(
                    color = Color(0xFFF04B32),
                    radius = lhWidth * 0.3f,
                    center = Offset(lhLeft + lhWidth*0.5f, height * 0.15f)
                )

                // Radar waves (orange)
                drawArc(
                    color = Color(0xFFF7941D),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(lhLeft - 40f, height * 0.1f),
                    size = Size(30f, 30f),
                    style = Stroke(width = 4f)
                )
                drawArc(
                    color = Color(0xFFF7941D),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(lhLeft - 50f, height * 0.05f),
                    size = Size(50f, 50f),
                    style = Stroke(width = 4f)
                )
                
                // Light beam
                val beamPath = Path().apply {
                    moveTo(lhRight, height * 0.2f)
                    lineTo(lhRight + 30f, height * 0.18f)
                    lineTo(lhRight + 30f, height * 0.22f)
                    close()
                }
                drawPath(beamPath, color = Color(0xFFF7941D))

                // Buildings (Teal)
                val bldg1Left = lhRight + 5f
                val bldg1Bottom = height * 0.75f
                drawRect(
                    color = Color(0xFF00B2D6),
                    topLeft = Offset(bldg1Left, height * 0.45f),
                    size = Size(20f, bldg1Bottom - height * 0.45f)
                )
                // Building roof
                val roof1Path = Path().apply {
                    moveTo(bldg1Left, height * 0.45f)
                    lineTo(bldg1Left + 20f, height * 0.35f)
                    lineTo(bldg1Left + 20f, height * 0.45f)
                    close()
                }
                drawPath(roof1Path, color = Color(0xFF00B2D6))

                val bldg2Left = bldg1Left + 22f
                drawRect(
                    color = Color(0xFF0096B4),
                    topLeft = Offset(bldg2Left, height * 0.35f),
                    size = Size(20f, bldg1Bottom - height * 0.35f)
                )
                val roof2Path = Path().apply {
                    moveTo(bldg2Left, height * 0.35f)
                    lineTo(bldg2Left + 20f, height * 0.25f)
                    lineTo(bldg2Left + 20f, height * 0.35f)
                    close()
                }
                drawPath(roof2Path, color = Color(0xFF0096B4))

                // Boat
                val boatLeft = width * 0.15f
                val boatBottom = height * 0.7f
                // Hull
                val hullPath = Path().apply {
                    moveTo(boatLeft, boatBottom - 10f)
                    lineTo(boatLeft + 30f, boatBottom - 10f)
                    lineTo(boatLeft + 25f, boatBottom)
                    lineTo(boatLeft + 5f, boatBottom)
                    close()
                }
                drawPath(hullPath, color = Color(0xFFF7941D))
                // Sail
                val sailPath = Path().apply {
                    moveTo(boatLeft + 10f, boatBottom - 12f)
                    lineTo(boatLeft + 10f, boatBottom - 35f)
                    lineTo(boatLeft - 5f, boatBottom - 12f)
                    close()
                }
                drawPath(sailPath, color = Color(0xFF00A651))

                // Splash
                drawCircle(color = Color(0xFFF37053), radius = 6f, center = Offset(width * 0.1f, height * 0.85f))
                drawCircle(color = Color(0xFFEC407A), radius = 8f, center = Offset(width * 0.15f, height * 0.78f))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "KAKINADA",
            color = Color(0xFF003876),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "SMART CITY",
            color = Color(0xFF00B2D6),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.height(1.dp).width(40.dp).background(Color.Gray))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TOWARDS A BETTER TOMORROW",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.height(1.dp).width(40.dp).background(Color.Gray))
        }
    }
}
