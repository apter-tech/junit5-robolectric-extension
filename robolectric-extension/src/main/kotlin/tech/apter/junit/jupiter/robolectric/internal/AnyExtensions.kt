package tech.apter.junit.jupiter.robolectric.internal

import org.junit.platform.commons.logging.LoggerFactory

internal fun Any.createLogger() = LoggerFactory.getLogger(javaClass)
