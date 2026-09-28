package com.example

import com.example.core.FlutterErrorHandlingMapping
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFlutterErrorHandlingMappingConstants() {
    assertEquals("FlutterError.onError", FlutterErrorHandlingMapping.FLUTTER_ERROR_ON_ERROR)
    assertEquals("PlatformDispatcher.instance.onError", FlutterErrorHandlingMapping.PLATFORM_DISPATCHER_ON_ERROR)
    assertEquals("runZonedGuarded()", FlutterErrorHandlingMapping.RUN_ZONED_GUARDED)
  }

  @Test
  fun testSetupGlobalErrorHandlersRunsSuccessfully() {
    try {
      FlutterErrorHandlingMapping.setupGlobalErrorHandlers()
      assertTrue(true)
    } catch (e: Exception) {
      fail("setupGlobalErrorHandlers threw an exception: ${e.message}")
    }
  }
}
