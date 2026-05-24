/*
 * Copyright 2025 pinorobotics
 * 
 * Website: https://github.com/pinorobotics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pinorobotics.cogni.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.community.mcp.server.transport.StdioMcpServerTransport;
import dev.langchain4j.mcp.protocol.McpImplementation;
import id.xfunction.cli.CommandOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import pinorobotics.cogni.PoseDatabase;
import pinorobotics.cogni.Ros2Bridge;
import pinorobotics.cogni.tools.HandTeachingTool;
import pinorobotics.cogni.tools.LabelingTool;
import pinorobotics.cogni.tools.ListingTool;
import pinorobotics.cogni.tools.MoveTool;

/**
 * @author aeon aeon_flux@eclipso.ch
 */
public class CogniApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CogniApp.class);
    private CommandOptions options;

    public CogniApp(CommandOptions options) {
        this.options = options;
    }

    @SuppressWarnings("resource")
    static void usage() throws IOException {
        Scanner scanner =
                new Scanner(CogniApp.class.getResource("/README-cogni.md").openStream())
                        .useDelimiter("\n");
        while (scanner.hasNext()) System.out.println(scanner.next());
    }

    public static void main(String[] args) throws Exception {
        try {
            var options = CommandOptions.collectOptions(args);
            options.populateFromFile("propertiesFile");
            LOGGER.info("CLI options={}", options);
            if (options.getOption("h").isPresent() || options.getOption("help").isPresent()) {
                usage();
            }
            var action =
                    options.getOption(CogniOptions.ACTION)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Please specify which one of the actions to"
                                                            + " perform (use '-h' for help)"));
            switch (action) {
                case CogniOptions.START_MCP -> new CogniApp(options).startMcp();
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // some MCP clients (like StdioMcpTransport) may ignore stderr, in such cases the only
            // way to report errors
            // is by sending them to stdout
            System.out.println(e);
            System.exit(1);
        }
    }

    private void setupMcpLogging() {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            options.getOption(CogniOptions.LOG_FILE)
                    .ifPresent(logFile -> System.setProperty("LOG_FILE", logFile));
            configurator.setContext(context);
            configurator.doConfigure(CogniApp.class.getResourceAsStream("/logback-mcp.xml"));
        } catch (JoranException e) {
            e.printStackTrace();
        }
    }

    private void startMcp() throws InterruptedException {
        LOGGER.info("Start MCP");

        McpImplementation serverInfo = new McpImplementation();
        serverInfo.setName("cogni-mcp-server");

        PoseDatabase poseDb = new PoseDatabase();
        Ros2Bridge ros =
                new Ros2Bridge(
                        options.getRequiredOption(CogniOptions.CONTROLLER_NAME),
                        options.getOption(CogniOptions.JOINT_STATE_TOPIC)
                                .orElse(CogniOptions.DEFAULT_JOINT_STATE_TOPIC));
        ros.start();
        var tools = new ArrayList<>();
        tools.add(new LabelingTool(poseDb, ros));
        tools.add(new ListingTool(poseDb));
        // TODO
        tools.add(
                new MoveTool(
                        List.of("Joint_0", "Joint_1", "Joint_2", "Joint_3", "Joint_4"),
                        poseDb,
                        ros));
        if (options.isOptionTrue(CogniOptions.R2D2_CONTROLLER))
            tools.add(
                    new HandTeachingTool(
                            poseDb,
                            ros,
                            options.getOptionMillis(
                                            CogniOptions.HAND_TEACHING_CAPTURE_RATE_IN_MILLIS)
                                    .orElse(
                                            CogniOptions
                                                    .DEFAULT_HAND_TEACHING_CAPTURE_RATE_IN_MILLIS)));
        McpServer server = new McpServer(tools, serverInfo);
        setupMcpLogging();
        new StdioMcpServerTransport(System.in, System.out, server).awaitClose();
    }
}
