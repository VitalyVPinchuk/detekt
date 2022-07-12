@file:JvmName("CustomConfig")

package io.gitlab.arturbosch.detekt.generator

import com.beust.jcommander.JCommander
import io.github.detekt.tooling.internal.NotApiButProbablyUsedByUsers
import io.gitlab.arturbosch.detekt.cli.parseArguments
import io.gitlab.arturbosch.detekt.cli.runners.Executable
import io.gitlab.arturbosch.detekt.cli.runners.Runner
import kotlin.system.exitProcess
import java.io.PrintStream
import java.nio.file.Files

object _CustomConfig {
    @Suppress("detekt.SpreadOperator")
    @JvmStatic
    fun _main(args: Array<String>) {
        val options = GeneratorArgsReduced()
        val parser = JCommander(options)
        parser.parse(*args)

        if (options.help) {
            parser.usage()
            exitProcess(0)
        }

        require(Files.isDirectory(options.configPath)) { "Config path must be a directory." }

        Generator(options).executeCustomConfig()
    }
}

fun main(args: Array<String>) {
    val options = GeneratorArgsReduced()
    val parser = JCommander(options)
    parser.parse(*args)

    if (options.help) {
        parser.usage()
        exitProcess(0)
    }

    require(Files.isDirectory(options.configPath)) { "Config path must be a directory." }

    Generator(options).executeCustomConfig()
}

@NotApiButProbablyUsedByUsers
@Deprecated(
    "Don't build a runner yourself.",
    ReplaceWith(
        "DetektCli.load().run(args, outputPrinter, errorPrinter)",
        "io.github.detekt.tooling.api.DetektCli"
    )
)
fun buildRunner(
    args: Array<String>,
    outputPrinter: PrintStream,
    errorPrinter: PrintStream
): Executable {
    val arguments = parseArguments(args)
    return Runner(arguments, outputPrinter, errorPrinter)
}
