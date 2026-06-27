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

import java.util.List;
import java.util.Objects;

/**
 * Represents a recorded trajectory of joint positions.
 *
 * @param label the human-readable identifier for the trajectory
 * @param poses the sequence of joint configurations
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public record TrajectoryRecord(String label, List<PoseSample> poses) {
    /**
     * Validates the trajectory record.
     *
     * @throws IllegalArgumentException if label is null/empty or poses list is empty
     */
    public TrajectoryRecord {
        Objects.requireNonNull(label, "Label cannot be null");
        if (label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }
        if (poses == null || poses.isEmpty()) {
            throw new IllegalArgumentException("Trajectory must contain at least one pose");
        }
    }

    @Override
    public String toString() {
        return """
        {
          "label": %s,
          "first": %s,
          "last": %s
        }
        """
                .formatted(
                        label,
                        poses.size() < 1 ? "none" : poses.getFirst(),
                        poses.size() < 1 ? "none" : poses.getLast());
    }
}
