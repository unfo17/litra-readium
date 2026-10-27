package io.github.unfo17.litra

import io.github.unfo17.litra.data.*
import org.junit.Assert.*
import org.junit.Test

class BookRulesTest {
    private fun book(status: String = "READING", progress: Double = 0.0) = Book(title = "Test", authors = "", fileName = "test.epub", hash = "test", status = status, progress = progress)
    @Test fun endIs99AndNeedsManualCompletion() {
        assertEquals(0.99, BookRules.progress(1.0), 0.0)
        assertEquals("99%", book(progress = BookRules.progress(1.0)).progressLabel)
        assertTrue(book(progress = 0.99).needsFinish)
        assertFalse(book(progress = 0.98).needsFinish)
    }
    @Test fun finishedSurvivesRereadingAndHasNoCompletionPrompt() {
        assertEquals("FINISHED", BookRules.readingStatus("FINISHED"))
        assertEquals("已读完", book("FINISHED", 0.5).progressLabel)
        assertFalse(book("FINISHED", 0.99).needsFinish)
        assertEquals("READING", BookRules.readingStatus("WANT_TO_READ"))
    }
    @Test fun invalidProgressIsBounded() {
        assertEquals(0.0, BookRules.progress(Double.NaN), 0.0)
        assertEquals(0.0, BookRules.progress(-1.0), 0.0)
        assertEquals(0.99, BookRules.progress(2.0), 0.0)
    }
}
