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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.TrajectoryRecord;

/**
 * In-memory implementation of PoseDatabase.
 *
 * @author aeon aeon_flux@eclipso.ch
 */
public class InMemoryPoseDatabase implements PoseDatabase {
    private final Map<String, PoseRecord> store = new HashMap<>();
    private final Map<String, TrajectoryRecord> trajectoryStore = new HashMap<>();

    @Override
    public void addPose(PoseRecord pose) throws LabelExistsException {
        if (store.containsKey(pose.label())) {
            throw new LabelExistsException("Pose with label '" + pose.label() + "' already exists");
        }
        store.put(pose.label(), pose);
    }

    @Override
    public void addTrajectory(TrajectoryRecord trajectory) throws LabelExistsException {
        if (trajectoryStore.containsKey(trajectory.label())) {
            throw new LabelExistsException(
                    "Trajectory with label '" + trajectory.label() + "' already exists");
        }
        trajectoryStore.put(trajectory.label(), trajectory);
    }

    @Override
    public Optional<PoseRecord> findPose(String label) {
        return Optional.ofNullable(store.get(label));
    }

    @Override
    public Optional<TrajectoryRecord> findTrajectory(String label) {
        return Optional.ofNullable(trajectoryStore.get(label));
    }

    @Override
    public List<PoseRecord> getSortedPoses() {
        List<PoseRecord> list = new ArrayList<>(store.values());
        list.sort((p1, p2) -> p1.label().compareTo(p2.label()));
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<TrajectoryRecord> getSortedTrajectories() {
        List<TrajectoryRecord> list = new ArrayList<>(trajectoryStore.values());
        list.sort((t1, t2) -> t1.label().compareTo(t2.label()));
        return Collections.unmodifiableList(list);
    }

    @Override
    public void deletePose(String label) {
        store.remove(label);
    }

    @Override
    public void deleteTrajectory(String label) {
        trajectoryStore.remove(label);
    }
}
