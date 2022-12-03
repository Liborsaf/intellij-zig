package org.ziglang.project

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import org.ziglang.ZigBundle
import org.ziglang.executeCommand
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JComboBox

val zigPath: String by lazy {
	PathEnvironmentVariableUtil.findInPath("zig")?.absolutePath
			?: when {
				SystemInfo.isWindows -> "C://Program Files/"
				SystemInfo.isMac -> "C://Program Files/" // TODO
				else -> "/usr/bin/zig"
			}
}

fun versionOf(path: String) = executeCommand(arrayOf(path, "version"))
		.first
		.firstOrNull()
		?: ZigBundle.message("zig.version.unknown")

fun findOrCreate(baseDir: VirtualFile, dir: String, module: Module) =
		baseDir.findChild(dir) ?: baseDir.createChildDirectory(module, dir)

fun validateZigExe(exePath: String) = Files.exists(Paths.get(exePath))

// https://github.com/ziglang/zig/blob/78f26b970e1c5c288e45d0edd2ab2f229e6fe924/src/introspect.zig#L8-L34
val someFiles = sequenceOf("zig.zig", "std.zig")
fun validateZigLib(libPath: String) = someFiles.any { file ->
	Files.exists(Paths.get(libPath, "lib", "zig", "std", file))
} || someFiles.any { file ->
        Files.exists(Paths.get(libPath, "lib", "std", file))
}

fun validateZigSDK(sdkHome: String) = Files.exists(Paths.get(sdkHome, "bin", "zig")) ||
		Files.exists(Paths.get(sdkHome, "bin", "zig.exe"))

fun LinkLabel<Any>.asLink() {
	setListener({ _, _ ->
		BrowserLauncher.instance.browse(text)
	}, null)
}

inline fun initExeComboBox(
		zigExeField: ComboboxWithBrowseButton,
		project: Project? = null,
		crossinline addListener: (ComboboxWithBrowseButton) -> Unit = {}) {
	zigExeField.addBrowseFolderListener(ZigBundle.message("zig.messages.run.select-compiler"),
			ZigBundle.message("zig.messages.run.select-compiler.description"),
			project,
			FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
			object : TextComponentAccessor<JComboBox<Any>> {
				override fun getText(component: JComboBox<Any>) = component.selectedItem as? String ?: ""
				override fun setText(component: JComboBox<Any>, text: String) {
					component.addItem(text)
					addListener(zigExeField)
				}
			})
	zigGlobalSettings.knownZigExes.forEach(zigExeField.comboBox::addItem)
}
