package Config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import de.rub.nds.tlsattacker.tls.config.ClientCommandConfig;
import de.rub.nds.tlsattacker.tls.config.converters.FileConverter;
import de.rub.nds.tlsattacker.tls.config.validators.PercentageValidator;
import java.io.File;
import java.util.logging.Logger;

/**
 * A Config File which controls the EvolutionaryFuzzer.
 * 
 * @author Robert Merget - robert.merget@rub.de
 * @author Juraj Somorovsky <juraj.somorovsky@rub.de>
 */
public class EvolutionaryFuzzerConfig extends ClientCommandConfig {

    /**
     *
     */
    public static final String ATTACK_COMMAND = "fuzzer";
    private static final Logger LOG = Logger.getLogger(EvolutionaryFuzzerConfig.class.getName());

    @Parameter(names = "-server_command_file", description = "Command for starting the server, initialized from a given File or Folder.", converter = FileConverter.class)
    private String serverCommandFromFile = "server/";
    @Parameter(names = "-output_folder", description = "Output folder for the fuzzing results.", converter = FileConverter.class)
    private String outputFolder = "./";
    @Parameter(names = "-threads", description = "Number of Threads running Simultaniously, (Default:Number of Server in Config)", validateWith = PositiveInteger.class)
    private Integer threads = -1;
    @Parameter(names = "-agent", description = "The Agent the Fuzzer uses to monitor the application (Default: AFL). Possible: AFL, PIN")
    private String agent = "AFL";
    @Parameter(names = "-mutator", description = "The Mutator the Fuzzer uses to generate new TestVectors (Default: simple). Possible: simple")
    private String mutator = "simple";
    @Parameter(names = "-certificate_mutator", description = "The Mutator the Fuzzer uses to generate new Certificates (Default: fixed). Possible: fixed")
    private String certMutator = "fixed";
    @Parameter(names = "-no_old", description = "The mutator wont run WorkflowTraces he finds in the Good WorkflowTrace Folder, before he starts generating new Mutations")
    private boolean noOld = false;
    @Parameter(names = "-start_stopped", description = "Starts the Fuzzer in a stopped state.")
    private boolean startStopped = false;
    @Parameter(names = "-clean_start", description = "Deletes previous good Workflows on startup")
    private boolean cleanStart = false;
    @Parameter(names = "-config_folder", description = "The Folder in which the config Files are", converter = FileConverter.class)
    private String configFolder = "config/";

    private File tracesFolder; // Temporary Folder which contains currently
			       // executed
    // traces
    private File crashedFolder; // Contains Traces which crashed the
    // Implementation
    private File timeoutFolder; // Contains Traces which timedout
    private File goodTracesFolder; // Contains Traces which look promising
    private File faultyFolder; // Contains Traces which caused an exception on

    public String getConfigFolder() {
	return configFolder;
    }

    public void setConfigFolder(String configFolder) {
	this.configFolder = configFolder;
    }

    public File getTracesFolder() {
	return tracesFolder;
    }

    public File getCrashedFolder() {
	return crashedFolder;
    }

    public File getTimeoutFolder() {
	return timeoutFolder;
    }

    public File getGoodTracesFolder() {
	return goodTracesFolder;
    }

    public File getFaultyFolder() {
	return faultyFolder;
    }

    public String getCertMutator() {
	return certMutator;
    }

    public void setCertMutator(String certMutator) {
	this.certMutator = certMutator;
    }

    public String getMutator() {
	return mutator;
    }

    public void setMutator(String mutator) {
	this.mutator = mutator;
    }

    public boolean isCleanStart() {
	return cleanStart;
    }

    public void setCleanStart(boolean cleanStart) {
	this.cleanStart = cleanStart;
    }

    public boolean isStartStopped() {
	return startStopped;
    }

    public void setStartStopped(boolean startStopped) {
	this.startStopped = startStopped;
    }

    public boolean isNoOld() {
	return noOld;
    }

    public void setNoOld(boolean noOld) {
	this.noOld = noOld;
    }

    // Are we currently in serialization mode?
    private boolean serialize = false;

    /**
     * Constructor for EvolutionaryFuzzerConfig, defaults output Folder to "."
     * and serverCommandFromFile to server/server.config
     */
    public EvolutionaryFuzzerConfig() {
	outputFolder = "data/";
	serverCommandFromFile = outputFolder + "server/";
	this.timeout = 10000;
	this.tlsTimeout = 100;
	setFuzzingMode(true);
	setKeystore("../resources/rsa1024.jks");
	setPassword("password");
	setAlias("alias");
	this.crashedFolder = new File(outputFolder + "crash/");
	this.faultyFolder = new File(outputFolder + "faulty/");
	this.goodTracesFolder = new File(outputFolder + "good/");
	this.tracesFolder = new File(outputFolder + "traces/");
	this.timeoutFolder = new File(outputFolder + "timeout/");
    }

    public boolean isSerialize() {
	return serialize;
    }

    public void setSerialize(boolean serialize) {
	this.serialize = serialize;
    }

    public String getAgent() {
	return agent;
    }

    public void setAgent(String agent) {
	this.agent = agent;
    }

    public Integer getThreads() {
	return threads;
    }

    public void setThreads(Integer threads) {
	this.threads = threads;
    }

    /**
     * Returns the path to the ServerConfig File
     * 
     * @return Path to the ServerConfig File
     */
    public String getServerCommandFromFile() {
	return serverCommandFromFile;
    }

    /**
     * Sets the path to the ServerConfig File
     * 
     * @param serverCommandFromFile
     */
    public void setServerCommandFromFile(String serverCommandFromFile) {
	this.serverCommandFromFile = serverCommandFromFile;
    }

    /**
     * Returns the Path to the Folder in which the Fuzzer will save its output
     * to. The Server will genereate several Folder in the Output Folder.
     * 
     * @return Path to the Folder in which the Fuzzer will save its output to
     */
    public String getOutputFolder() {
	return outputFolder;
    }

    /**
     * Sets the Path to the Folder in which the Fuzzer will save its output to.
     * The Server will genereate several Folder in the Output Folder.
     * 
     * @param outputFolder
     */
    public void setOutputFolder(String outputFolder) {
	this.outputFolder = outputFolder;
	this.crashedFolder = new File(outputFolder + "crash/");
	this.faultyFolder = new File(outputFolder + "faulty/");
	this.goodTracesFolder = new File(outputFolder + "good/");
	this.tracesFolder = new File(outputFolder + "traces/");
	this.timeoutFolder = new File(outputFolder + "timeout/");
	// if (!crashedFolder.exists() && !crashedFolder.mkdirs()) {
	// throw new RuntimeException("Could not Create Output Folder!");
	// }
	// if (!faultyFolder.exists() && !faultyFolder.mkdirs()) {
	// throw new RuntimeException("Could not Create Output Folder!");
	// }
	// if (!goodTracesFolder.exists() && !goodTracesFolder.mkdirs()) {
	// throw new RuntimeException("Could not Create Output Folder!");
	// }
	// if (!tracesFolder.exists() && !tracesFolder.mkdirs()) {
	// throw new RuntimeException("Could not Create Output Folder!");
	// }
	// if (!timeoutFolder.exists() && !timeoutFolder.mkdirs()) {
	// throw new RuntimeException("Could not Create Output Folder!");
	// }
    }

}
