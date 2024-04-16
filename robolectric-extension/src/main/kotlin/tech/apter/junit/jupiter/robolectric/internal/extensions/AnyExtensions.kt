package tech.apter.junit.jupiter.robolectric.internal.extensions

import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory

internal inline fun <reified T : Any> T.createLogger(): Logger = LoggerFactory.getLogger(T::class.java)
