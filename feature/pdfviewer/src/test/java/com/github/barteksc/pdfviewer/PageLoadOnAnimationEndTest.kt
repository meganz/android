package com.github.barteksc.pdfviewer

import android.animation.Animator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageLoadOnAnimationEndTest {

    private val pdfView: PDFView = mock()
    private val animator: Animator = mock()

    private lateinit var underTest: AnimationManager.PageLoadOnAnimationEnd

    @BeforeEach
    fun setUp() {
        reset(pdfView)
        underTest = AnimationManager.PageLoadOnAnimationEnd(pdfView)
    }

    @Test
    fun `test that onAnimationEnd calls loadPages when animation ends normally`() {
        underTest.onAnimationEnd(animator)

        verify(pdfView).loadPages()
    }

    @Test
    fun `test that onAnimationEnd does not call loadPages when animation is cancelled`() {
        underTest.onAnimationCancel(animator)
        underTest.onAnimationEnd(animator)

        verify(pdfView, never()).loadPages()
    }

    @Test
    fun `test that onAnimationCancel does not call loadPages`() {
        underTest.onAnimationCancel(animator)

        verify(pdfView, never()).loadPages()
    }

    @Test
    fun `test that onAnimationEnd calls loadPages after cancel when new animation starts`() {
        underTest.onAnimationCancel(animator)
        underTest.onAnimationEnd(animator)
        verify(pdfView, never()).loadPages()

        underTest.onAnimationStart(animator)
        underTest.onAnimationEnd(animator)
        verify(pdfView).loadPages()
    }
}
