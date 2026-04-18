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

import id.xfunction.XJson;
import java.util.Objects;

/**
 * Immutable record representing a stored pose for the robotic arm. Contains the joint angles and a
 * human-readable label for the pose.
 */
public record PoseRecord(String label, double[] jointAngles) {

    /**
     * Validates that the joint angles array has exactly 5 elements (for a 5-DoF arm).
     *
     * @throws IllegalArgumentException if the joint angles array is null or has incorrect size
     */
    public PoseRecord {
        Objects.requireNonNull(jointAngles, "Joint angles cannot be null");
        if (jointAngles.length != 5) {
            throw new IllegalArgumentException("Expected 5 joint angles for a 5-DoF arm");
        }
    }

    /**
     * Creates a new PoseRecord with the given label and joint angles.
     *
     * @param label The human-readable name for this pose
     * @param jointAngles Array of 5 double values representing joint positions
     * @return New PoseRecord instance
     */
    public static PoseRecord of(String label, double[] jointAngles) {
        return new PoseRecord(label, jointAngles);
    }

    /**
     * Creates a new PoseRecord with the given label and default joint angles (all zeros).
     *
     * @param label The human-readable name for this pose
     * @return New PoseRecord instance with zeroed joint angles
     */
    public static PoseRecord of(String label) {
        return new PoseRecord(label, new double[] {0.0, 0.0, 0.0, 0.0, 0.0});
    }

    /**
     * Creates a new PoseRecord with updated joint angles.
     *
     * @param updatedAngles New array of joint angles
     * @return New PoseRecord instance with updated angles
     */
    public PoseRecord withUpdatedAngles(double[] updatedAngles) {
        return new PoseRecord(label, updatedAngles);
    }

    @Override
    public String toString() {
        return XJson.asString(
                "label", label,
                "jointAngles", jointAngles);
    }
}
