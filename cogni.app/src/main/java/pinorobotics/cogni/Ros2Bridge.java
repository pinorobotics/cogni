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
package pinorobotics.cogni;

import id.jros2client.JRos2Client;
import id.jros2client.JRos2ClientConfiguration;
import id.jros2client.JRos2ClientFactory;
import id.jros2messages.sensor_msgs.JointStateMessage;
import id.jros2messages.trajectory_msgs.JointTrajectoryMessage;
import id.jrosclient.TopicSubmissionPublisher;
import id.jrosclient.TopicSubscriber;
import id.jroscommon.RosName;
import id.jrosmessages.trajectory_msgs.JointTrajectoryPointMessage;
import id.xfunction.Preconditions;
import id.xfunction.util.IdempotentService;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge to ROS 2 for the Cogni AI‑powered robotic arm.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Subscription to joint‑state updates. The bridge also stores the latest joint angles for
 *       later retrieval.
 *   <li>Publication of trajectory commands
 *   <li>Query of the latest joint angles
 * </ul>
 *
 * <p>All ROS 2 communication is performed via the {@code jrosclient} library.
 */
public class Ros2Bridge extends IdempotentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ros2Bridge.class);

    private final JRos2Client rosClient;
    private final RosName jointStateTopic;
    private final RosName trajCommandTopic;

    private TopicSubscriber<JointStateMessage> subscriber;
    private TopicSubmissionPublisher<JointTrajectoryMessage> publisher;
    private volatile double[] latestJointAngles = new double[5];

    /**
     * Constructor with custom topic names.
     *
     * @param jointStateTopic ROS 2 topic to subscribe for {@code sensor_msgs/JointState}
     * @param controllerName ROS 2 joint trajectory controller name
     */
    public Ros2Bridge(String controllerName, String jointStateTopic) {
        this.jointStateTopic = new RosName(jointStateTopic);
        this.trajCommandTopic = new RosName(controllerName).add("joint_trajectory");
        var configBuilder = new JRos2ClientConfiguration.Builder();
        this.rosClient = new JRos2ClientFactory().createClient(configBuilder.build());
    }

    /**
     * @return a copy of the most recent joint angles (radians).
     */
    public double[] getCurrentJointAngles() {
        return latestJointAngles;
    }

    /**
     * Publish a trajectory command to the arm.
     *
     * @param trajectory trajectory message (must contain at least one point)
     * @throws IllegalArgumentException if the trajectory violates joint limits or format
     */
    public void publishTrajectory(JointTrajectoryMessage trajectory) {
        validateTrajectory(trajectory);
        publisher.submit(trajectory);
    }

    /** Basic safety validation – checks NaN/Infinity values. */
    private void validateTrajectory(JointTrajectoryMessage trajectory) {
        JointTrajectoryPointMessage[] points = trajectory.points;
        Preconditions.isTrue(
                points != null && points.length > 0, "Trajectory must contain at least one point");
        JointTrajectoryPointMessage point = points[0];
        double[] positions = point.positions;
        Preconditions.isTrue(
                positions != null && positions.length == 5,
                "Trajectory point must have exactly 5 positions");
        for (int i = 0; i < 5; i++) {
            double pos = positions[i];
            Preconditions.isTrue(
                    !Double.isNaN(pos) && !Double.isInfinite(pos),
                    "Invalid position at joint " + i + ": " + pos);
        }
    }

    @Override
    protected void onClose() {
        subscriber.getSubscription().ifPresent(Subscription::cancel);
        publisher.close();
    }

    @Override
    protected void onStart() {
        subscriber =
                new TopicSubscriber<>(JointStateMessage.class, jointStateTopic.name()) {
                    @Override
                    public void onNext(JointStateMessage msg) {
                        LOGGER.debug("New joint state {}", msg);
                        try {
                            double[] positions = msg.position;
                            Preconditions.isTrue(
                                    positions != null && positions.length == 5,
                                    "Expected 5 joint positions; got "
                                            + (positions == null ? "null" : positions.length));
                            latestJointAngles = positions;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            getSubscription().get().request(1);
                        }
                    }
                };
        rosClient.subscribe(subscriber);

        publisher =
                new TopicSubmissionPublisher<>(
                        JointTrajectoryMessage.class, trajCommandTopic.name());
        rosClient.publish(publisher);
    }
}
