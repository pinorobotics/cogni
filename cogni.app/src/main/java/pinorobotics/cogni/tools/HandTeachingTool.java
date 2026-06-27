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
import id.xfunction.Preconditions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pinorobotics.cogni.PoseSample;
import pinorobotics.cogni.Ros2Bridge;
import pinorobotics.cogni.TrajectoryRecord;
import pinorobotics.cogni.db.LabelExistsException;
import pinorobotics.cogni.db.PoseDatabase;

/**
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public class HandTeachingTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandTeachingTool.class);

    private final PoseDatabase poseDatabase;
    private final Ros2Bridge ros2Bridge;

    private volatile boolean teachingInProgress = false;
    private volatile String activeLabel;
    private volatile Thread recordingThread;
    private Duration captureRate;
    private final List<PoseSample> trajectoryBuffer = new ArrayList<>();

    public HandTeachingTool(
            PoseDatabase poseDatabase, Ros2Bridge ros2Bridge, Duration captureRate) {
        this.poseDatabase = poseDatabase;
        this.ros2Bridge = ros2Bridge;
        this.captureRate = captureRate;
    }

    @Tool("Start hand‑teaching for a given label. The user can manually guide the arm.")
    public String start(@P("label for a new trajectory") String label) {
        Preconditions.notNull(label);
        LOGGER.debug("Start hand teaching");
        if (teachingInProgress) {
            return "Hand‑teaching already in progress. Stop it before starting a new session.";
        }
        if (label == null || label.isBlank()) {
            return "Label cannot be empty.";
        }
        if (poseDatabase.findTrajectory(label).isPresent())
            throw new LabelExistsException("Trajectory with label '" + label + "' already exists");
        teachingInProgress = true;
        activeLabel = label;
        // Disable motors so the user can manually move the arm
        ros2Bridge.startMotors(false);
        // Periodically capture joint angles
        recordingThread =
                new Thread(
                        () -> {
                            try {
                                while (teachingInProgress
                                        && !Thread.currentThread().isInterrupted()) {
                                    double[] angles = ros2Bridge.getCurrentJointAngles();
                                    Preconditions.notNull(angles);
                                    Preconditions.equals(angles.length, 5, "getCurrentJointAngles");
                                    PoseSample sample =
                                            new PoseSample(
                                                    angles[0], angles[1], angles[2], angles[3],
                                                    angles[4]);
                                    if (!trajectoryBuffer.isEmpty()) {
                                        if (!Arrays.equals(
                                                trajectoryBuffer.getLast().jointAngles(), angles)) {
                                            trajectoryBuffer.add(sample);
                                        } else {
                                            LOGGER.debug("Pose did not change, ignoring it");
                                        }
                                    } else {
                                        trajectoryBuffer.add(sample);
                                    }
                                    Thread.sleep(captureRate.toMillis());
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
        recordingThread.start();
        return "Ready to capture trajectory for label '" + label + "'";
    }

    @Tool("Stop hand‑teaching for a label and save the recorded trajectory")
    public String stop(@P("label of current trajectory") String label) {
        Preconditions.notNull(label);
        LOGGER.debug("Stop hand teaching");
        if (!teachingInProgress || !activeLabel.equals(label)) {
            return "No active hand teaching for label '" + label + "'";
        }
        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        var trajectory = new TrajectoryRecord(label, trajectoryBuffer);
        poseDatabase.addTrajectory(trajectory);
        teachingInProgress = false;
        int wayPointCount = trajectoryBuffer.size();
        trajectoryBuffer.clear();
        activeLabel = null;
        // Re‑enable motors
        ros2Bridge.startMotors(true);
        return "Trajectory '" + label + "' saved with " + wayPointCount + " distinct poses.";
    }
}
