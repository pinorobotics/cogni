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
    private static final int MAX_ENTRIES = 100;
    private final Map<String, PoseRecord> entries;

    /** Creates a new PoseDatabase instance. */
    public PoseDatabase() {
        this.entries = new HashMap<>();
    }

    /**
     * Adds a new pose to the database.
     *
     * @param poseRecord The pose record to store
     * @throws IllegalStateException if the database is full
     */
    public void addPose(PoseRecord poseRecord) {
        if (entries.size() >= MAX_ENTRIES) {
            throw new IllegalStateException("Maximum number of stored poses reached");
        }
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
     * Updates an existing pose.
     *
     * @param label The label of the pose to update
     * @param newPoseRecord The new pose record
     * @throws IllegalArgumentException if the label doesn't exist
     */
    public void update(String label, PoseRecord newPoseRecord) {
        if (!entries.containsKey(label)) {
            throw new IllegalArgumentException("Label '" + label + "' not found");
        }
        entries.put(label, newPoseRecord);
    }

    /**
     * Removes a pose by its label.
     *
     * @param label The label of the pose to remove
     * @return true if the pose was removed, false if it didn't exist
     */
    public boolean remove(String label) {
        return entries.remove(label) != null;
    }

    /**
     * Gets the current size of the database.
     *
     * @return Number of entries in the database
     */
    public int size() {
        return entries.size();
    }

    /** Clears all entries from the database. */
    public void clear() {
        entries.clear();
    }

    /**
     * Checks if the database contains a specific label.
     *
     * @param label The label to check
     * @return true if the label exists, false otherwise
     */
    public boolean containsLabel(String label) {
        return entries.containsKey(label);
    }

    public List<PoseRecord> getSortedPoses() {
        return entries.values().stream().sorted(Comparator.comparing(PoseRecord::label)).toList();
    }
}
