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
import id.jros2messages.trajectory_msgs.JointTrajectoryMessage;
import id.jrosmessages.std_msgs.StringMessage;
import id.jrosmessages.trajectory_msgs.JointTrajectoryPointMessage;
import java.util.List;
import pinorobotics.cogni.PoseDatabase;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.Ros2Bridge;

/**
 * Tool that moves the robotic arm to a previously stored pose. The LLM invokes this tool by
 * supplying a semantic label that was stored earlier (e.g. "light switch ON").
 */
public class MoveTool {

    private final PoseDatabase poseDatabase;
    private final Ros2Bridge ros2Bridge;
    private StringMessage[] jointNames;

    public MoveTool(List<String> jointNames, PoseDatabase poseDatabase, Ros2Bridge ros2Bridge) {
        this.jointNames =
                jointNames.stream().map(StringMessage::new).toArray(s -> new StringMessage[s]);
        this.poseDatabase = poseDatabase;
        this.ros2Bridge = ros2Bridge;
    }

    /**
     * Moves the arm to the pose identified by {@code label}.
     *
     * @param label the semantic label that was previously stored
     * @return a short status message
     */
    @Tool("Move to the pose labeled `label`")
    public String moveTo(@P("label") String label) {
        // Retrieve stored pose; may be null if label does not exist
        PoseRecord record = poseDatabase.get(label).orElse(null);
        if (record == null) {
            throw new IllegalArgumentException("Label '" + label + "' not found");
        }

        // Build a trajectory message that drives the arm to the saved joint angles

        var point = new JointTrajectoryPointMessage();
        point.velocities = new double[5];
        point.effort = new double[5];
        point.time_from_start.sec = 2; // 2‑second travel time

        // Copy the stored joint angles into the trajectory point.
        // PoseRecord is expected to expose the five joint angles via getters.
        point.positions = record.jointAngles();

        // Publish the trajectory command (the Ros2Bridge adds safety validation).
        ros2Bridge.publishTrajectory(
                new JointTrajectoryMessage().withJointNames(jointNames).withPoints(point));

        return "Moving to stored pose: " + record;
    }
}
