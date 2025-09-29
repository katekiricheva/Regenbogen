package de.regenbogen

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import javax.swing.JPanel
import javax.swing.SwingUtilities
import java.awt.*
import java.awt.event.HierarchyEvent
import java.io.File
import com.intellij.ui.components.JBScrollPane

class MindMapPanel(project: Project) : JPanel(), Disposable {

    private val nodes = mutableListOf<String>()
    private val padding = 10
    private val verticalSpacing = 40
    private var onContentChanged: (() -> Unit)? = null

    var nodeCount: Int = 0
        private set

    private var pendingScrollToBottom = false

    init {
        project.service<DebuggerEventService>().subscribe(this) { message: String ->
            addNode(message)
        }

        addHierarchyListener { e ->
            if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && isShowing && pendingScrollToBottom) {
                SwingUtilities.invokeLater {
                    (SwingUtilities.getAncestorOfClass(JBScrollPane::class.java, this) as? JBScrollPane)?.let { sp ->
                        scrollToBottom(sp)
                    }
                }
                pendingScrollToBottom = false
            }
        }
    }

    fun addNode(text: String) {
        EventQueue.invokeLater {
            nodes.add(text)
            nodeCount = nodes.size
            updatePreferredSize()
            revalidate()
            repaint()
            onContentChanged?.invoke()

            val scrollPane = SwingUtilities.getAncestorOfClass(JBScrollPane::class.java, this) as? JBScrollPane
            if (scrollPane != null && scrollPane.isShowing) {
                scrollToBottom(scrollPane)
                pendingScrollToBottom = false
            } else {
                pendingScrollToBottom = true
            }
        }
    }

    fun reset() {
        nodes.clear()
        nodeCount = 0
        updatePreferredSize()
        repaint()
        onContentChanged?.invoke()
    }

    private fun scrollToBottom(sp: JBScrollPane) {
        SwingUtilities.invokeLater {
            sp.revalidate()
            sp.viewport.view?.revalidate()

            SwingUtilities.invokeLater {
                val view = sp.viewport.view
                if (view != null) {
                    val h = view.height
                    scrollRectToVisible(Rectangle(0, (h - 1).coerceAtLeast(0), 1, 1))
                }
            }
        }
    }

    fun setOnContentChanged(callback: (() -> Unit)?) {
        this.onContentChanged = callback
    }

    fun isEmpty(): Boolean = nodeCount == 0

    private fun updatePreferredSize() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val fm = getFontMetrics(scheme.getFont(EditorFontType.PLAIN))

        var y = 50
        var maxWidth = 0

        for (node in nodes) {
            val textWidth = fm.stringWidth(node)
            val boxWidth = textWidth + padding * 6
            val boxHeight = fm.height + padding

            if (boxWidth > maxWidth) {
                maxWidth = boxWidth
            }

            y += boxHeight + verticalSpacing
        }

        if (maxWidth == 0) {
            maxWidth = 200
        }

        preferredSize = Dimension(maxWidth, y)
        revalidate()
    }

    fun exportToFile(file: File) {
        file.writeText(nodes.joinToString("\n"))
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val scheme = EditorColorsManager.getInstance().globalScheme
        g2.font = scheme.getFont(EditorFontType.PLAIN)
        val fm = g2.fontMetrics

        if (nodes.isEmpty()) {
            val msg = "Start debugger first"
            val textWidth = fm.stringWidth(msg)
            val x = (width - textWidth) / 2
            val y = height / 2
            g2.color = JBColor.foreground()
            g2.drawString(msg, x, y)
            return
        }

        var y = 50

        for ((index, node) in nodes.withIndex()) {
            val textWidth = fm.stringWidth(node)
            val textHeight = fm.height
            val boxWidth = textWidth + padding * 2
            val boxHeight = textHeight + padding
            val x = (width - boxWidth) / 2

            g2.color = JBColor.border()
            g2.drawRoundRect(x, y, boxWidth, boxHeight, 15, 15)

            g2.color = JBColor.foreground()
            g2.drawString(node, x + padding, y + fm.ascent + (padding / 2))

            if (index < nodes.size - 1) {
                g2.color = JBColor.border()
                val lineX = x + boxWidth / 2
                val lineY1 = y + boxHeight + 5
                val lineY2 = y + boxHeight + verticalSpacing - 5
                g2.drawLine(lineX, lineY1, lineX, lineY2)
            }

            y += boxHeight + verticalSpacing
        }
    }

    override fun dispose() {}
}