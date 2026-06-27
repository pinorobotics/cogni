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
package pinorobotics.cogni.db;

import java.util.List;
import java.util.Optional;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.TrajectoryRecord;

/**
 * Core contract for pose and trajectory persistence.
 *
 * <p>The implementation can be in‑memory or persisted via JPA (SQLite). All methods are
 * deliberately simple and thread‑safe.
 *
 * @author aeon aeon_flux@eclipso.ch
 */
public interface PoseDatabase {

    /**
     * Stores or updates a pose record.
     *
     * @param pose the record to store; label must be non‑null and not empty
     */
    void addPose(PoseRecord pose) throws LabelExistsException;

    /**
     * Stores or updates a trajectory record.
     *
     * @param trajectory the record to store; trajectory label must be unique
     */
    void addTrajectory(TrajectoryRecord trajectory) throws LabelExistsException;

    /**
     * Removes a pose entry by its label. If the label does not exist, this operation is idempotent
     * (no exception thrown).
     *
     * @param label the label of the pose to delete
     */
    void deletePose(String label);

    /**
     * Removes a trajectory entry by its label. If the label does not exist, this operation is
     * idempotent (no exception thrown).
     *
     * @param label the label of the trajectory to delete
     */
    void deleteTrajectory(String label);

    /**
     * Retrieves a pose by its label.
     *
     * @param label the label to look up
     * @return an {@link Optional} containing the {@link PoseRecord} if present
     */
    Optional<PoseRecord> findPose(String label);

    /**
     * Retrieves a trajectory by its label.
     *
     * @param label the label to look up
     * @return an {@link Optional} containing the {@link TrajectoryRecord} if present
     */
    Optional<TrajectoryRecord> findTrajectory(String label);

    /**
     * Returns all stored pose records sorted by their label.
     *
     * @return an immutable list of pose records
     */
    List<PoseRecord> getSortedPoses();

    /**
     * Returns all stored trajectory records sorted by their label.
     *
     * @return an immutable list of trajectory records
     */
    List<TrajectoryRecord> getSortedTrajectories();
}
