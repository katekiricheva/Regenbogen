package de.regenbogen

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.components.service

class PostStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<DebuggerEventService>()
    }
}