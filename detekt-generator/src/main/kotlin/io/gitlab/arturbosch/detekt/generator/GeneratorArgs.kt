package io.gitlab.arturbosch.detekt.generator

import com.beust.jcommander.Parameter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class GeneratorArgs {
    abstract val inputPath: List<Path>
    abstract val configPath: Path
    open val documentationPath: Path = Paths.get("")
    open val cliOptionsPath: Path = Paths.get("")
}

class GeneratorArgsFull : GeneratorArgs() {

    @Parameter(
        names = ["--input", "-i"],
        required = true,
        description = "Input paths to analyze."
    )
    private var input: String? = null

    @Parameter(
        names = ["--documentation", "-d"],
        required = true,
        description = "Output path for generated documentation."
    )
    private var documentation: String? = null

    @Parameter(
        names = ["--config", "-c"],
        required = true,
        description = "Output path for generated detekt config."
    )
    private var config: String? = null

    @Parameter(
        names = ["--cli-options"],
        required = true,
        description = "Output path for generated cli options page."
    )
    private var cliOptions: String? = null

    @Parameter(
        names = ["--help", "-h"],
        help = true,
        description = "Shows the usage."
    )
    var help: Boolean = false

    override val inputPath: List<Path> by lazy {
        checkNotNull(input) { "Input parameter was not initialized by jcommander!" }
            .splitToSequence(",", ";")
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .map { first -> Paths.get(first) }
            .onEach { require(Files.exists(it)) { "Input path must exist!" } }
            .toList()
    }

    override val documentationPath: Path
        get() = Paths.get(
            checkNotNull(documentation) {
                "Documentation output path was not initialized by jcommander!"
            }
        )

    override val configPath: Path
        get() = Paths.get(checkNotNull(config) { "Configuration output path was not initialized by jcommander!" })

    override val cliOptionsPath: Path
        get() = Paths.get(
            checkNotNull(cliOptions) {
                "Cli options output path was not initialized by jcommander!"
            }
        )
}

class GeneratorArgsReduced : GeneratorArgs() {

    @Parameter(
        names = ["--input", "-i"],
        required = true,
        description = "Input paths to analyze."
    )
    private var input: String? = null

    @Parameter(
        names = ["--config", "-c"],
        required = true,
        description = "Output path for generated detekt config."
    )
    private var config: String? = null

    @Parameter(
        names = ["--help", "-h"],
        help = true,
        description = "Shows the usage."
    )
    var help: Boolean = false

    override val inputPath: List<Path> by lazy {
        checkNotNull(input) { "Input parameter was not initialized by jcommander!" }
            .splitToSequence(",", ";")
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .map { first -> Paths.get(first) }
            .onEach { require(Files.exists(it)) { "Input path must exist!" } }
            .toList()
    }

    override val configPath: Path
        get() = Paths.get(checkNotNull(config) { "Configuration output path was not initialized by jcommander!" })
}
