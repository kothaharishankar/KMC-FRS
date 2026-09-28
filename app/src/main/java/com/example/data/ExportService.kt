package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.AttendanceRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportService {

    fun exportCsv(context: Context, records: List<AttendanceRecord>): File {
        val csvHeader = "Record ID,Employee ID,Employee Name,Date,Time,Status,Late Duration,Location,Latitude,Longitude\n"
        val csvBody = records.joinToString("\n") { r ->
            val escapedName = escapeCsvField(r.employeeName)
            val escapedLocation = escapeCsvField(r.locationName)
            val lateDur = r.getFormattedLateDuration().ifEmpty { r.lateDuration ?: "" }
            "${r.id},${r.employeeId},$escapedName,${r.date},${r.time},${r.status},${escapeCsvField(lateDur)},$escapedLocation,${r.latitude},${r.longitude}"
        }
        val file = File(context.cacheDir, "KSCCL_Attendance_Report.csv")
        file.writeText(csvHeader + csvBody, Charsets.UTF_8)
        return file
    }

    fun exportExcel(context: Context, records: List<AttendanceRecord>): File {
        val sb = StringBuilder()
        sb.append("""
            <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
            <head>
            <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
            <style>
                table { border-collapse: collapse; width: 100%; font-family: sans-serif; }
                th { background-color: #1A56DB; color: white; font-weight: bold; padding: 10px; border: 1px solid #ddd; }
                td { padding: 8px; border: 1px solid #ddd; }
                tr:nth-child(even) { background-color: #f9f9f9; }
                .title { font-size: 18px; font-weight: bold; color: #1A56DB; margin-bottom: 10px; }
                .subtitle { font-size: 14px; color: #555; margin-bottom: 20px; }
            </style>
            </head>
            <body>
            <div class="title">KSCCL — Attendance Report</div>
            <div class="subtitle">Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</div>
            <table>
                <thead>
                    <tr>
                        <th>Record ID</th>
                        <th>Employee ID</th>
                        <th>Employee Name</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Status</th>
                        <th>Late Duration</th>
                        <th>Location</th>
                        <th>Latitude</th>
                        <th>Longitude</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())

        for (r in records) {
            val lateDur = r.getFormattedLateDuration().ifEmpty { r.lateDuration ?: "" }
            sb.append("""
                    <tr>
                        <td>${r.id}</td>
                        <td>${r.employeeId}</td>
                        <td>${r.employeeName}</td>
                        <td>${r.date}</td>
                        <td>${r.time}</td>
                        <td>${r.status}</td>
                        <td>$lateDur</td>
                        <td>${r.locationName}</td>
                        <td>${r.latitude}</td>
                        <td>${r.longitude}</td>
                    </tr>
            """.trimIndent())
        }

        sb.append("""
                </tbody>
            </table>
            </body>
            </html>
        """.trimIndent())

        val file = File(context.cacheDir, "KSCCL_Attendance_Report.xls")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun exportPdf(context: Context, records: List<AttendanceRecord>): File {
        val pdfDocument = PdfDocument()
        
        // Page dimensions
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        
        // Margins
        val margin = 40f
        val titleY = 60f
        val orgY = 80f
        val lineY = 95f
        
        val tableHeaderY = 120f
        val tableHeaderHeight = 25f
        val rowHeight = 22f
        
        var currentY = tableHeaderY + tableHeaderHeight
        var pageNumber = 1
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A56DB")
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val orgPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isAntiAlias = true
        }
        
        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 1f
        }
        
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#1A56DB")
            style = Paint.Style.FILL
        }
        
        val stripeBgPaint = Paint().apply {
            color = Color.parseColor("#F4F6F9")
            style = Paint.Style.FILL
        }
        
        val statusPresentPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val statusLatePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val statusAbsentPaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val drawHeadersAndFooter: (Canvas, Int) -> Unit = { c, pageNum ->
            // Draw Header
            c.drawText("KSCCL — Attendance Report", margin, titleY, titlePaint)
            c.drawText("Kakinada Smart City Corporation Limited (KSCCL)", margin, orgY, orgPaint)
            c.drawLine(margin, lineY, pageWidth - margin, lineY, linePaint)
            
            // Draw Footer
            val footerY = pageHeight - 30f
            c.drawLine(margin, footerY - 10, pageWidth - margin, footerY - 10, linePaint)
            c.drawText("Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", margin, footerY, textPaint)
            val pageStr = "Page $pageNum"
            val pageBounds = Rect()
            textPaint.getTextBounds(pageStr, 0, pageStr.length, pageBounds)
            c.drawText(pageStr, pageWidth - margin - pageBounds.width(), footerY, textPaint)
            
            // Draw Table Header
            c.drawRect(margin, tableHeaderY, pageWidth - margin, tableHeaderY + tableHeaderHeight, headerBgPaint)
            
            // Column positions
            val colX = floatArrayOf(margin + 5, margin + 65, margin + 185, margin + 255, margin + 325, margin + 405)
            c.drawText("Emp ID", colX[0], tableHeaderY + 16, headerTextPaint)
            c.drawText("Employee Name", colX[1], tableHeaderY + 16, headerTextPaint)
            c.drawText("Date", colX[2], tableHeaderY + 16, headerTextPaint)
            c.drawText("Time", colX[3], tableHeaderY + 16, headerTextPaint)
            c.drawText("Status", colX[4], tableHeaderY + 16, headerTextPaint)
            c.drawText("Location", colX[5], tableHeaderY + 16, headerTextPaint)
        }
        
        drawHeadersAndFooter(canvas, pageNumber)
        
        val colX = floatArrayOf(margin + 5, margin + 65, margin + 185, margin + 255, margin + 325, margin + 405)
        
        for (i in records.indices) {
            val r = records[i]
            
            if (currentY + rowHeight > pageHeight - 50f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeadersAndFooter(canvas, pageNumber)
                currentY = tableHeaderY + tableHeaderHeight
            }
            
            if (i % 2 == 1) {
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, stripeBgPaint)
            }
            
            canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, linePaint)
            
            val textY = currentY + 14f
            canvas.drawText(r.employeeId, colX[0], textY, boldTextPaint)
            
            var displayName = r.employeeName
            if (displayName.length > 22) displayName = displayName.substring(0, 19) + "..."
            canvas.drawText(displayName, colX[1], textY, textPaint)
            
            canvas.drawText(r.date, colX[2], textY, textPaint)
            canvas.drawText(r.time, colX[3], textY, textPaint)
            
            val statusText = if (r.status == "LATE") {
                val lateDur = r.getFormattedLateDuration()
                if (lateDur.isNotEmpty()) lateDur else "LATE"
            } else {
                r.status
            }
            
            val statusPaint = when (r.status) {
                "PRESENT" -> statusPresentPaint
                "LATE" -> statusLatePaint
                "ABSENT" -> statusAbsentPaint
                else -> textPaint
            }
            
            var displayStatus = statusText
            if (displayStatus.length > 15) displayStatus = displayStatus.substring(0, 12) + "..."
            canvas.drawText(displayStatus, colX[4], textY, statusPaint)
            
            var displayLoc = r.locationName
            if (displayLoc.length > 20) displayLoc = displayLoc.substring(0, 17) + "..."
            canvas.drawText(displayLoc, colX[5], textY, textPaint)
            
            currentY += rowHeight
        }
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "KSCCL_Attendance_Report.pdf")
        try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "Share Attendance Report")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}
