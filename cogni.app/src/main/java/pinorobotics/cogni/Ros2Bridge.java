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
import id.jrosclient.TopicSubscriber;
import id.jroscommon.RosName;
import id.jrosmessages.trajectory_msgs.JointTrajectoryPointMessage;
import id.xfunction.Preconditions;
import id.xfunction.retry.RetryException;
import id.xfunction.retry.RetryableExecutor;
import id.xfunction.util.IdempotentService;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pinorobotics.jros2actionlib.JRos2ActionLibFactory;
import pinorobotics.jros2control.control_msgs.FollowJointTrajectoryActionDefinition;
import pinorobotics.jros2control.control_msgs.FollowJointTrajectoryGoalMessage;
import pinorobotics.jros2control.control_msgs.FollowJointTrajectoryResultMessage;
import pinorobotics.jros2services.JRos2ServiceClient;
import pinorobotics.jros2services.JRos2ServicesFactory;
import pinorobotics.jrosactionlib.JRosActionClient;
import pinorobotics.jrosservices.std_srvs.TriggerRequestMessage;
import pinorobotics.jrosservices.std_srvs.TriggerResponseMessage;
import pinorobotics.jrosservices.std_srvs.TriggerServiceDefinition;

/**
 * Bridge to ROS 2 for the Cogni AI‑powered robotic arm.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Subscription to joint‑state updates. The bridge also stores the latest joint angles for
 *       later retrieval.
 *   <li>Publication of trajectory commands via an action server
 *   <li>Query of the latest joint angles
 * </ul>
 *
 * <p>All ROS 2 communication is performed via the {@code jrosclient} library.
 */
public class Ros2Bridge extends IdempotentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ros2Bridge.class);

    private final JRos2Client rosClient;
    private final RosName jointStateTopic;
    private final RosName actionServerName;
    private final RosName motorTriggerService;

    private TopicSubscriber<JointStateMessage> subscriber;
    private JRosActionClient<FollowJointTrajectoryGoalMessage, FollowJointTrajectoryResultMessage>
            actionClient;
    private JRos2ServiceClient<TriggerRequestMessage, TriggerResponseMessage> motorServiceClient;
    private volatile double[] latestJointAngles = new double[5];

    /**
     * Constructor with custom topic names.
     *
     * @param jointStateTopic ROS 2 topic to subscribe for {@code sensor_msgs/JointState}
     * @param controllerName ROS 2 joint trajectory controller name
     */
    public Ros2Bridge(String controllerName, String jointStateTopic) {
        this.jointStateTopic = new RosName(jointStateTopic);
        this.actionServerName =
                new RosName(controllerName)
                        .add("trajectory_controller")
                        .add("follow_joint_trajectory");
        this.motorTriggerService = new RosName(controllerName).add("motor_trigger_service");
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
     * Send a trajectory command to the ROS 2 action server.
     *
     * @param trajectory trajectory message (must contain at least one point)
     * @throws IllegalArgumentException if the trajectory violates joint limits or format
     */
    public void sendTrajectory(JointTrajectoryMessage trajectory) {
        LOGGER.debug("Send trajectory 1");
        validateTrajectory(trajectory);
        // ← use the action client to send the goal
        try {
            LOGGER.debug("Send trajectory {}", trajectory);
            actionClient
                    .sendGoalAsync(
                            new FollowJointTrajectoryGoalMessage().withTrajectory(trajectory))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startMotors(boolean isOn) {
        // Build request (TriggerRequestMessage has no fields)
        var request = new TriggerRequestMessage();

        new RetryableExecutor()
                .retry(
                        () -> {
                            try {
                                // Send request and wait for completion
                                var response = motorServiceClient.sendRequestAsync(request).get();
                                LOGGER.debug("Trigger service response: {}", response);
                                Preconditions.isTrue(response.success);
                                Preconditions.equals(response.message.data, "Motor isOn=" + isOn);
                                return null;
                            } catch (Exception e) {
                                LOGGER.warn(e.getMessage());
                                throw new RetryException(e);
                            }
                        },
                        Duration.ofMillis(500),
                        3);
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
        try {
            motorServiceClient.close();
            actionClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        actionClient =
                new JRos2ActionLibFactory()
                        .createClient(
                                rosClient,
                                new FollowJointTrajectoryActionDefinition(),
                                actionServerName.name());

        // Use the TriggerServiceDefinition to issue a trigger request
        var triggerServiceDefinition = new TriggerServiceDefinition();
        motorServiceClient =
                new JRos2ServicesFactory()
                        .createClient(rosClient, triggerServiceDefinition, motorTriggerService);
    }
}
