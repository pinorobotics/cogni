/*
 * Copyright 2026 pinorobotics
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
package pinorobotics.cogni.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pinorobotics.cogni.PoseDatabase;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.Ros2Bridge;

/**
 * Tool that stores the current robot configuration under a semantic label.
 *
 * <p>The LLM invokes this method when the user asks to “assign a label to the current pose”. It
 * retrieves the latest joint angles from the ROS‑2 bridge, builds a {@link PoseRecord}, and
 * persists it via the {@link PoseDatabase}.
 */
public class LabelingTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabelingTool.class);
    private final PoseDatabase poseDatabase;
    private final Ros2Bridge rosBridge;

    public LabelingTool(PoseDatabase poseDatabase, Ros2Bridge rosBridge) {
        this.poseDatabase = poseDatabase;
        this.rosBridge = rosBridge;
    }

    /**
     * Stores the current joint configuration under the supplied label.
     *
     * @param label a human‑readable identifier (e.g. “light switch ON”)
     * @return a confirmation message for the user
     */
    @Tool(
            "Store the current robot configuration under a semantic label and return all joint"
                    + " angles for it.")
    public String storePose(@P("label") String label) {
        LOGGER.debug("Save label {}", label);
        // 1️⃣  Obtain the latest joint angles from the ROS‑2 bridge.
        double[] currentAngles = rosBridge.getCurrentJointAngles();

        // 2️⃣  Build a PoseRecord containing the label and the joint values.
        PoseRecord record = new PoseRecord(label, currentAngles);

        // 3️⃣  Persist the pose (the database overwrites an existing label if needed).
        poseDatabase.add(label, record);

        // 4️⃣  Return a friendly confirmation.
        return "Stored pose " + record;
    }
}
