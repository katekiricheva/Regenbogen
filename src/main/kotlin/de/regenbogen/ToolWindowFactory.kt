package de.regenbogen

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.components.BorderLayoutPanel
import java.io.File
import javax.swing.*
import java.awt.FlowLayout
import java.awt.FileDialog

class ToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mindMapPanel = MindMapPanel(project)

        val scrollPane = JBScrollPane(mindMapPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ).apply {
            border = BorderFactory.createEmptyBorder()
        }

        val resetButton = JButton("Reset").apply {
            isFocusable = false
            addActionListener {
                val optionPane = JOptionPane(
                    "Are you sure you want to reset the progress?",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION
                )

                val dialog = optionPane.createDialog(mindMapPanel, "Confirm Reset")

                dialog.setLocationRelativeTo(null)

                dialog.isVisible = true

                val result = optionPane.value
                if (result == JOptionPane.YES_OPTION || result == 0) {
                    mindMapPanel.reset()
                }
            }
        }
        val exportButton = JButton("Export").apply {
            isFocusable = false
            addActionListener {
                val dialog = FileDialog(null as java.awt.Frame?, "Export Debug Log", FileDialog.SAVE)
                dialog.file = "debug_log.txt"
                dialog.isVisible = true

                if (dialog.file != null) {
                    var file = File(dialog.directory, dialog.file)
                    // гарантируем .txt
                    if (!file.name.lowercase().endsWith(".txt")) {
                        file = File(file.parentFile, "${file.name}.txt")
                    }
                    mindMapPanel.exportToFile(file)
                }
            }
        }
        val toolbarPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(resetButton)
            add(exportButton)
        }
        val mainPanel = BorderLayoutPanel().apply {
            addToCenter(scrollPane)
            addToBottom(toolbarPanel)
        }

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        fun updateToolbarVisibility() {
            val empty = mindMapPanel.isEmpty()
            resetButton.isVisible = !empty
            exportButton.isVisible = !empty
            toolbarPanel.isVisible = !empty
        }

        mindMapPanel.setOnContentChanged { updateToolbarVisibility() }
        updateToolbarVisibility()
    }
}