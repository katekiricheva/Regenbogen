package de.regenbogen

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.xdebugger.*
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener

@Service(Service.Level.PROJECT)
class DebuggerEventService(private val project: Project) : Disposable {
    private val connection = project.messageBus.connect(this)
    private val eventLog = mutableListOf<String>()
    private val subscribers = mutableListOf<(String) -> Unit>()

    private var firstStackFrameSeen: Boolean = false
    private var lastFrameKey: String? = null

    init {
        connection.subscribe(XDebuggerManager.TOPIC, object : XDebuggerManagerListener {
            override fun processStarted(debugProcess: XDebugProcess) {
                val session = debugProcess.session
                notifyAll("▶ Debugger started at ${getCurrentFileName(session)}")

                session.addSessionListener(object : XDebugSessionListener {
                    override fun sessionPaused() {
                        val currentFrame = session.currentStackFrame

                        if (currentFrame != null) {
                            val currentFile = getCurrentFileName(session)
                            val currentLine = getCurrentLineNumber(session)

                            val currentFrameKey = "$currentFile:$currentLine"
                            val stackChanged = if (!firstStackFrameSeen) {
                                firstStackFrameSeen = true
                                false
                            } else {
                                currentFrameKey != lastFrameKey
                            }
                            lastFrameKey = currentFrameKey

                            if (stackChanged) {
                                notifyAll("↕ Paused (stack frame changed) at $currentFile:$currentLine")
                            } else {
                                notifyAll("⏸ Paused at $currentFile:$currentLine")
                            }
                        } else {
                            notifyAll("⏸ Paused at breakpoint (no stack frame)")
                        }
                    }

                    override fun beforeSessionResume() {
                        notifyAll("⏯ Resumed at ${getCurrentFileName(session)}:${getCurrentLineNumber(session)}")
                    }
                }, this@DebuggerEventService)
            }

            override fun processStopped(debugProcess: XDebugProcess) {
                val session = debugProcess.session
                notifyAll("⏹ Debugger stopped at ${getCurrentFileName(session)}")
            }
        })
    }

    fun subscribe(parentDisposable: Disposable, consumer: (String) -> Unit) {
        synchronized(subscribers) { subscribers += consumer }
        Disposer.register(parentDisposable) { synchronized(subscribers) { subscribers.remove(consumer) } }

        val snapshot: List<String>
        synchronized(eventLog) { snapshot = eventLog.toList() }
        ApplicationManager.getApplication().invokeLater {
            snapshot.forEach { consumer(it) }
        }
    }

    private fun notifyAll(message: String) {
        synchronized(eventLog) { eventLog += message }
        val snapshot = synchronized(subscribers) { subscribers.toList() }
        ApplicationManager.getApplication().invokeLater {
            snapshot.forEach { it(message) }
        }
    }

    private fun getCurrentFileName(session: XDebugSession): String {
        session.currentPosition?.file?.let { return it.name }
        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
        editor?.let {
            val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(it.document)
            file?.let { f -> return f.name }
        }
        val openFiles = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFiles
        if (openFiles.isNotEmpty()) {
            PsiManager.getInstance(project).findFile(openFiles[0])?.let { return it.name }
        }
        return "NoFile"
    }

    private fun getCurrentLineNumber(session: XDebugSession): String {
        session.currentPosition?.line?.let { return (it + 1).toString() }

        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
        editor?.let {
            val caretLine = it.caretModel.logicalPosition.line
            return (caretLine + 1).toString()
        }

        val openFiles = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFiles
        if (openFiles.isNotEmpty()) {
            val psiFile = PsiManager.getInstance(project).findFile(openFiles[0])
            psiFile?.viewProvider?.document?.let { doc ->
                if (doc.lineCount > 0) return "1"
            }
        }
        return "NoLine"
    }

    override fun dispose() {
        connection.dispose()
    }
}