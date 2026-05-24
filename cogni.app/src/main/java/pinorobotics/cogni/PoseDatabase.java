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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory database for storing and retrieving pose labels and their corresponding joint states.
 */
public final class PoseDatabase {
    private final Map<String, PoseRecord> entries = new HashMap<>();
    private final Map<String, TrajectoryRecord> trajectoryStore = new HashMap<>();

    /**
     * Adds a new pose to the database.
     *
     * @param poseRecord The pose record to store
     * @throws IllegalStateException if the database is full
     */
    public void addPose(PoseRecord poseRecord) {
        entries.put(poseRecord.label(), poseRecord);
    }

    /**
     * Retrieves a pose by its label.
     *
     * @param label The label of the pose to retrieve
     * @return Optional containing the pose record if found, empty otherwise
     */
    public Optional<PoseRecord> findPose(String label) {
        return Optional.ofNullable(entries.get(label));
    }

    /**
     * Adds a new trajectory to the database.
     *
     * @param trajectoryRecord The trajectory record to store
     * @throws IllegalStateException if the database is full
     */
    public void addTrajectory(TrajectoryRecord trajectoryRecord) {
        trajectoryStore.put(trajectoryRecord.label(), trajectoryRecord);
    }

    /**
     * Retrieves a trajectory by its label.
     *
     * @param label The label of the trajectory to retrieve
     * @return Optional containing the trajectory record if found, empty otherwise
     */
    public Optional<TrajectoryRecord> findTrajectory(String label) {
        return Optional.ofNullable(trajectoryStore.get(label));
    }

    /** Clears all entries from the database. */
    public void clear() {
        entries.clear();
        trajectoryStore.clear();
    }

    public List<PoseRecord> getSortedPoses() {
        return entries.values().stream().sorted(Comparator.comparing(PoseRecord::label)).toList();
    }

    /**
     * Gets all trajectories sorted by label.
     *
     * @return List of trajectory records sorted by label
     */
    public List<TrajectoryRecord> getSortedTrajectories() {
        return trajectoryStore.values().stream()
                .sorted(Comparator.comparing(TrajectoryRecord::label))
                .toList();
    }
}
