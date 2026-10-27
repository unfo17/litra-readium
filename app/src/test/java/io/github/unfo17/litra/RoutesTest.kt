package io.github.unfo17.litra
import io.github.unfo17.litra.ui.routes
import org.junit.Assert.assertEquals
import org.junit.Test
class RoutesTest { @Test fun tenRoutes() { assertEquals(10, routes.distinct().size) } }
