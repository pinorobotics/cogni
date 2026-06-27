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
import id.xfunction.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.PoseSample;
import pinorobotics.cogni.Ros2Bridge;
import pinorobotics.cogni.db.PoseDatabase;

/**
 * Tool that moves the robotic arm to a previously stored pose. The LLM invokes this tool by
 * supplying a semantic label that was stored earlier (e.g. "light switch ON").
 */
public class MoveTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoveTool.class);
    private final PoseDatabase db;
    private final Ros2Bridge ros2Bridge;
    private StringMessage[] jointNames;

    public MoveTool(List<String> jointNames, PoseDatabase poseDatabase, Ros2Bridge ros2Bridge) {
        this.jointNames =
                jointNames.stream().map(StringMessage::new).toArray(s -> new StringMessage[s]);
        this.db = poseDatabase;
        this.ros2Bridge = ros2Bridge;
    }

    /**
     * Moves the arm through the sequence of poses specified by {@code labels}.
     *
     * @param label the semantic label that was previously stored
     * @return a short status message
     */
    @Tool("Move robotic arm through the sequence of poses specified by labels")
    public String moveTo(@P("list of labels") List<String> labels) {
        for (var label : labels) {
            LOGGER.debug("Move to label {}", label);
            // Retrieve stored pose; may be null if label does not exist
            PoseRecord record = db.findPose(label).orElse(null);
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

            // Send the trajectory command (the Ros2Bridge adds safety validation).
            ros2Bridge.sendTrajectory(
                    new JointTrajectoryMessage().withJointNames(jointNames).withPoints(point));
        }
        return labels.size() == 1
                ? "Moved to stored pose: " + labels.getFirst()
                : "Moved through the sequence of poses: " + labels;
    }

    /**
     * Replays a previously recorded trajectory identified by its label.
     *
     * @param label the semantic label of the saved trajectory
     * @return a short status message
     */
    @Tool("Replay a stored trajectory")
    public String replay(@P("trajectory label") String label) {
        Preconditions.notNull(label);
        LOGGER.debug("Replaying trajectory label {}", label);
        var trajectory = db.findTrajectory(label).orElse(null);
        if (trajectory == null)
            throw new IllegalArgumentException("Trajectory label '" + label + "' not found");

        // Build a ROS2 JointTrajectory message containing all points of the trajectory
        var points = new ArrayList<JointTrajectoryPointMessage>();
        for (PoseSample poseSample : trajectory.poses()) {
            JointTrajectoryPointMessage point = new JointTrajectoryPointMessage();
            point.positions = poseSample.jointAngles();
            point.velocities = new double[5];
            point.effort = new double[5];
            // Simple incremental timing – 2 seconds per point
            point.time_from_start.sec = (int) poseSample.timestamp().getEpochSecond();
            points.add(point);
        }

        JointTrajectoryMessage trajectoryMsg =
                new JointTrajectoryMessage()
                        .withJointNames(jointNames)
                        .withPoints(points.toArray(new JointTrajectoryPointMessage[0]));

        // Send the trajectory to the arm
        ros2Bridge.sendTrajectory(trajectoryMsg);

        return "Replayed trajectory '%s' with %d points"
                .formatted(label, trajectoryMsg.points.length);
    }
}
