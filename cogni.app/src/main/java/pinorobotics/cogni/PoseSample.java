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
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record representing a single pose for the robotic arm.
 *
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public record PoseSample(Instant timestamp, double... jointAngles) {

    /**
     * Validates that the joint angles array has exactly 5 elements (for a 5-DoF arm).
     *
     * @throws IllegalArgumentException if the joint angles array is null or has incorrect size
     */
    public PoseSample {
        Objects.requireNonNull(jointAngles, "Joint angles cannot be null");
        if (jointAngles.length != 5) {
            throw new IllegalArgumentException("Expected 5 joint angles for a 5-DoF arm");
        }
    }

    public PoseSample(double... jointAngles) {
        this(Instant.now(), jointAngles);
    }

    @Override
    public String toString() {
        return XJson.asString(
                "timestamp", timestamp,
                "jointAngles", jointAngles);
    }
}
