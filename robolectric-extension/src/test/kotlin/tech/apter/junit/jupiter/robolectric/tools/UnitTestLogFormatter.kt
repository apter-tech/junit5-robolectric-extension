package tech.apter.junit.jupiter.robolectric.tools

import java.io.PrintWriter
import java.io.StringWriter
import java.text.MessageFormat
import java.util.Date
import java.util.logging.Formatter
import java.util.logging.LogRecord

class UnitTestLogFormatter : Formatter() {
    private val messageFormat = MessageFormat("[{3,date,MM-dd hh:mm:ss.SSS} | {2} | {0}] {4} {5}\n")

    override fun format(logRecord: LogRecord): String {
        val stackTrace = logRecord.thrown?.let { throwable ->
            StringWriter().use { stringWriter ->
                PrintWriter(stringWriter).use { printWriter ->
                    printWriter.println()
                    throwable.printStackTrace(printWriter)
                }
                stringWriter.toString()
            }
        } ?: ""

        return messageFormat.format(
            arrayOf(
                logRecord.loggerName.substringAfterLast('.'),
                logRecord.level,
                Thread.currentThread().name,
                Date(logRecord.millis),
                formatMessage(logRecord),
                stackTrace,
            )
        )
    }
}
