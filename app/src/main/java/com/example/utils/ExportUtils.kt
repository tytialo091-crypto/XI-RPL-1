package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.model.MonthlyReportSummary
import com.example.domain.model.PeminjamanDetail
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Utilitas ekspor laporan bulanan dan riwayat peminjaman ke PDF, Excel (.xls), serta berbagi berkas.
 */
object ExportUtils {

    /**
     * Menulis laporan lengkap ke OutputStream berformat PDF menggunakan Android PdfDocument.
     */
    fun writePdfReport(
        outputStream: OutputStream,
        summary: MonthlyReportSummary,
        loans: List<PeminjamanDetail>
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(2, 132, 199) // #0284C7 Deep Sky Blue
            style = Paint.Style.FILL
        }
        val lightBoxPaint = Paint().apply {
            color = Color.rgb(224, 242, 254) // #E0F2FE Light Sky Blue
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(224, 242, 254)
            textSize = 11f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(186, 230, 253)
            strokeWidth = 1f
        }

        // Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 86f, headerPaint)
        canvas.drawText("EDU-PINJAM — LAPORAN INVENTARIS SEKOLAH", 32f, 36f, titlePaint)
        canvas.drawText(
            "Periode: ${summary.bulanLabel}  •  Dicetak: ${DateUtils.formatTanggalLengkap(System.currentTimeMillis())}",
            32f,
            58f,
            subtitlePaint
        )

        var y = 110f

        // Ringkasan Metrik
        canvas.drawRoundRect(32f, y, pageWidth - 32f, y + 66f, 10f, 10f, lightBoxPaint)
        canvas.drawText("Ringkasan Peminjaman", 44f, y + 20f, sectionPaint)
        canvas.drawText(
            "Total Transaksi: ${summary.totalTransaksi}   |   Dikembalikan: ${summary.totalDikembalikan}   |   Terlambat: ${summary.totalTerlambat}   |   Denda: ${DateUtils.formatRupiah(summary.totalDendaTerkumpul)}",
            44f,
            y + 44f,
            bodyPaint
        )
        y += 88f

        // Barang Paling Sering Dipinjam
        canvas.drawText("Barang Paling Sering Dipinjam", 32f, y, sectionPaint)
        y += 18f
        if (summary.topBarang.isEmpty()) {
            canvas.drawText("- Belum ada data peminjaman.", 40f, y, mutedPaint)
            y += 16f
        } else {
            summary.topBarang.take(5).forEachIndexed { idx, item ->
                canvas.drawText(
                    "${idx + 1}. ${item.namaBarang} (${item.kodeBarang}) — ${item.totalDipinjamKali}x dipinjam (${item.totalUnitDipinjam} unit)",
                    40f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
        }
        y += 10f

        // Siswa Paling Aktif
        canvas.drawText("Siswa Paling Aktif Meminjam", 32f, y, sectionPaint)
        y += 18f
        if (summary.topSiswa.isEmpty()) {
            canvas.drawText("- Belum ada data siswa peminjam.", 40f, y, mutedPaint)
            y += 16f
        } else {
            summary.topSiswa.take(5).forEachIndexed { idx, siswa ->
                canvas.drawText(
                    "${idx + 1}. ${siswa.namaSiswa} (${siswa.kelas}) — ${siswa.totalTransaksi} transaksi (${siswa.totalUnitDipinjam} unit)",
                    40f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
        }
        y += 14f

        // Daftar Transaksi
        canvas.drawText("Daftar Riwayat Peminjaman", 32f, y, sectionPaint)
        y += 12f
        canvas.drawLine(32f, y, pageWidth - 32f, y, dividerPaint)
        y += 16f

        loans.forEachIndexed { index, detail ->
            if (y > pageHeight - 60) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 44f
            }
            val namaBarang = detail.barang?.namaBarang ?: "Barang"
            val namaSiswa = detail.peminjam?.nama ?: "Siswa"
            val tglPinjam = DateUtils.formatTanggalPendek(detail.peminjaman.tanggalPinjam)
            val tglKembali = DateUtils.formatTanggalPendek(detail.peminjaman.tanggalKembaliRencana)
            val status = detail.statusTampil.label
            val denda = if (detail.estimasiDendaLive > 0L) " | Denda: ${DateUtils.formatRupiah(detail.estimasiDendaLive)}" else ""

            canvas.drawText(
                "${index + 1}. [${detail.peminjaman.kodePeminjaman}] $namaBarang (${detail.peminjaman.jumlah} unit)",
                32f,
                y,
                bodyPaint
            )
            y += 14f
            canvas.drawText(
                "    Peminjam: $namaSiswa  •  $tglPinjam s/d $tglKembali  •  Status: $status$denda",
                32f,
                y,
                mutedPaint
            )
            y += 18f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    /**
     * Menulis laporan ke format Excel SpreadsheetML (.xls) agar dapat langsung dibuka Microsoft Excel / Sheets.
     */
    fun buildExcelXmlReport(
        summary: MonthlyReportSummary,
        loans: List<PeminjamanDetail>
    ): String {
        fun esc(text: String): String = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        return buildString {
            appendLine("""<?xml version="1.0"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""  <Worksheet ss:Name="Laporan_EduPinjam">""")
            appendLine("""    <Table>""")
            appendLine("""      <Row><Cell><Data ss:Type="String">LAPORAN PEMINJAMAN INVENTARIS EDU-PINJAM</Data></Cell></Row>""")
            appendLine("""      <Row><Cell><Data ss:Type="String">Periode: ${esc(summary.bulanLabel)}</Data></Cell></Row>""")
            appendLine("""      <Row><Cell><Data ss:Type="String">Kode Pinjam</Data></Cell><Cell><Data ss:Type="String">Barang</Data></Cell><Cell><Data ss:Type="String">Peminjam</Data></Cell><Cell><Data ss:Type="String">Kelas</Data></Cell><Cell><Data ss:Type="String">Jumlah</Data></Cell><Cell><Data ss:Type="String">Tgl Pinjam</Data></Cell><Cell><Data ss:Type="String">Tgl Kembali</Data></Cell><Cell><Data ss:Type="String">Status</Data></Cell><Cell><Data ss:Type="String">Denda (Rp)</Data></Cell><Cell><Data ss:Type="String">Keperluan</Data></Cell></Row>""")
            loans.forEach { d ->
                appendLine(
                    """      <Row>""" +
                        """<Cell><Data ss:Type="String">${esc(d.peminjaman.kodePeminjaman)}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(d.barang?.namaBarang ?: "-")}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(d.peminjam?.nama ?: "-")}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(d.peminjam?.kelas ?: "-")}</Data></Cell>""" +
                        """<Cell><Data ss:Type="Number">${d.peminjaman.jumlah}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(DateUtils.formatTanggalPendek(d.peminjaman.tanggalPinjam))}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(DateUtils.formatTanggalPendek(d.peminjaman.tanggalKembaliRencana))}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(d.statusTampil.label)}</Data></Cell>""" +
                        """<Cell><Data ss:Type="Number">${d.estimasiDendaLive}</Data></Cell>""" +
                        """<Cell><Data ss:Type="String">${esc(d.peminjaman.keperluan)}</Data></Cell>""" +
                        """</Row>"""
                )
            }
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

    /**
     * Membuat file sementara di cache untuk dibagikan melalui FileProvider.
     */
    fun createShareableReportFile(
        context: Context,
        fileName: String,
        writer: (FileOutputStream) -> Unit
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { out -> writer(out) }
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
}
