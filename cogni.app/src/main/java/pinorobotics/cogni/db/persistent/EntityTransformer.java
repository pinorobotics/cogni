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
package pinorobotics.cogni.db.persistent;

import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.PoseSample;
import pinorobotics.cogni.TrajectoryRecord;
import pinorobotics.cogni.db.persistent.entities.PoseRecordEntity;
import pinorobotics.cogni.db.persistent.entities.PoseSampleEntity;
import pinorobotics.cogni.db.persistent.entities.TrajectoryRecordEntity;

/**
 * @author aeon aeon_flux@eclipso.ch
 */
public class EntityTransformer {

    public PoseSample toPoseSample(PoseSampleEntity entity) {
        return new PoseSample(
                entity.getTimestamp(),
                entity.getJoint1(),
                entity.getJoint2(),
                entity.getJoint3(),
                entity.getJoint4(),
                entity.getJoint5());
    }

    public PoseRecord toPoseRecord(PoseRecordEntity entity) {
        return new PoseRecord(entity.getLabel(), toPoseSample(entity.getPose()));
    }

    public TrajectoryRecord toTrajectoryRecord(TrajectoryRecordEntity entity) {
        return new TrajectoryRecord(
                entity.getLabel(), entity.getSamples().stream().map(this::toPoseSample).toList());
    }

    public PoseRecordEntity toPoseRecordEntity(PoseRecord pose) {
        return new PoseRecordEntity(
                pose.label(),
                new PoseSampleEntity(
                        pose.jointAngles()[0],
                        pose.jointAngles()[1],
                        pose.jointAngles()[2],
                        pose.jointAngles()[3],
                        pose.jointAngles()[4],
                        pose.sample().timestamp()));
    }

    public TrajectoryRecordEntity toTrajectoryRecordEntity(TrajectoryRecord trajectory) {
        return new TrajectoryRecordEntity(
                trajectory.label(),
                trajectory.poses().stream()
                        .map(
                                p ->
                                        new PoseSampleEntity(
                                                p.jointAngles()[0],
                                                p.jointAngles()[1],
                                                p.jointAngles()[2],
                                                p.jointAngles()[3],
                                                p.jointAngles()[4],
                                                p.timestamp()))
                        .toList());
    }
}
