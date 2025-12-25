package kenny.fitbitkotlin

import kenny.fitbitkotlin.calories.CaloriesExporter
import kenny.fitbitkotlin.distance.DistanceExporter
import kenny.fitbitkotlin.heartrate.HeartRateExporter
import kenny.fitbitkotlin.sleep.SleepExporter
import kenny.fitbitkotlin.steps.StepsExporter
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

/**
 * Represents an Apple Health Record for XML generation.
 */
data class AppleHealthRecord(
    val type: String,
    val sourceName: String,
    val unit: String,
    val value: String,
    val creationDate: String,
    val startDate: String,
    val endDate: String
)

/**
 * Base interface for Apple Health XML exporters.
 * Mirrors the Importer<T> pattern but generates XML output.
 */
interface Exporter<T> {
    val sourceName: String
        get() = "Fitbit-dump"

    val appleHealthDateFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

    fun healthKitType(): String
    fun unit(): String
    fun queryData(from: LocalDateTime, to: LocalDateTime): List<T>
    fun toRecord(entity: T): AppleHealthRecord

    fun formatDate(dateTime: LocalDateTime): String {
        return dateTime.atOffset(ZoneOffset.UTC).format(appleHealthDateFormatter)
    }

    fun export(from: LocalDateTime, to: LocalDateTime, output: OutputStream): Int {
        val data = queryData(from, to)
        val records = data.map { toRecord(it) }
        AppleHealthXmlWriter.write(records, output)
        return records.size
    }
}

/**
 * Utility for writing Apple Health XML export format.
 */
object AppleHealthXmlWriter {

    fun write(records: List<AppleHealthRecord>, output: OutputStream) {
        val factory = XMLOutputFactory.newInstance()
        val writer = factory.createXMLStreamWriter(output, "UTF-8")

        writer.writeStartDocument("UTF-8", "1.0")
        writer.writeCharacters("\n")
        writer.writeDTD("<!DOCTYPE HealthData>")
        writer.writeCharacters("\n")
        writer.writeStartElement("HealthData")
        writer.writeAttribute("locale", "en_US")
        writer.writeCharacters("\n")

        for (record in records) {
            writeRecord(writer, record)
        }

        writer.writeEndElement()
        writer.writeEndDocument()
        writer.flush()
        writer.close()
    }

    private fun writeRecord(writer: XMLStreamWriter, record: AppleHealthRecord) {
        writer.writeCharacters("  ")
        writer.writeEmptyElement("Record")
        writer.writeAttribute("type", record.type)
        writer.writeAttribute("sourceName", record.sourceName)
        if (record.unit.isNotEmpty()) {
            writer.writeAttribute("unit", record.unit)
        }
        writer.writeAttribute("creationDate", record.creationDate)
        writer.writeAttribute("startDate", record.startDate)
        writer.writeAttribute("endDate", record.endDate)
        writer.writeAttribute("value", record.value)
        writer.writeCharacters("\n")
    }
}

@RestController
@RequestMapping("/api/export")
class ExportController(
    private val heartRateExporter: HeartRateExporter,
    private val stepsExporter: StepsExporter,
    private val caloriesExporter: CaloriesExporter,
    private val distanceExporter: DistanceExporter,
    private val sleepExporter: SleepExporter
) {

    @GetMapping("/heartrate")
    fun exportHeartRate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ByteArray> {
        return exportToXml("heartrate", from, to) { output ->
            heartRateExporter.export(from, to, output)
        }
    }

    @GetMapping("/steps")
    fun exportSteps(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ByteArray> {
        return exportToXml("steps", from, to) { output ->
            stepsExporter.export(from, to, output)
        }
    }

    @GetMapping("/calories")
    fun exportCalories(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ByteArray> {
        return exportToXml("calories", from, to) { output ->
            caloriesExporter.export(from, to, output)
        }
    }

    @GetMapping("/distance")
    fun exportDistance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ByteArray> {
        return exportToXml("distance", from, to) { output ->
            distanceExporter.export(from, to, output)
        }
    }

    @GetMapping("/sleep")
    fun exportSleep(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<ByteArray> {
        return exportToXml("sleep", from, to) { output ->
            sleepExporter.export(from, to, output)
        }
    }

    private fun exportToXml(
        dataType: String,
        from: LocalDateTime,
        to: LocalDateTime,
        exportFn: (ByteArrayOutputStream) -> Int
    ): ResponseEntity<ByteArray> {
        val output = ByteArrayOutputStream()
        exportFn(output)

        val filename = "apple-health-${dataType}-${from.toLocalDate()}-to-${to.toLocalDate()}.xml"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_XML)
            .body(output.toByteArray())
    }
}
