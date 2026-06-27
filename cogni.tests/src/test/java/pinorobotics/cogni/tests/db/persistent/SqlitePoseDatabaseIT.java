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
package pinorobotics.cogni.tests.db.persistent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.PoseSample;
import pinorobotics.cogni.TrajectoryRecord;
import pinorobotics.cogni.db.persistent.SqlitePoseDatabase;

/**
 * Integration tests for {@link SqlitePoseDatabase}.
 *
 * <p>These tests use a temporary SQLite file to verify the persistence implementation works as
 * expected.
 *
 * @author aeon
 */
public class SqlitePoseDatabaseIT {

    private static Path dbFile;
    private static SqlitePoseDatabase db;

    @BeforeEach
    void init() throws IOException {
        // create a temporary SQLite file that will be deleted on JVM exit
        dbFile = Files.createTempFile("cogni-test-db-", ".db");
        // dbFile.toFile().deleteOnExit();

        // initialize the database with the temporary file
        db = new SqlitePoseDatabase(dbFile);
    }

    @AfterEach
    void tearDown() {
        db.close();
        db = null;
    }

    @Test
    void addAndFindPose() {
        // create a simple pose record
        var poseSample = new PoseSample(Instant.EPOCH, 0.1, 0.2, 0.3, 0.4, 0.5);

        // Build a domain PoseRecord that contains the sample.
        // The constructor signature is assumed to be: PoseRecord(label, List<PoseSample>).
        var poseRecord = new PoseRecord("pose-001", poseSample);

        db.addPose(poseRecord);

        // Retrieve the same record
        Optional<PoseRecord> found = db.findPose("pose-001");
        assertTrue(found.isPresent(), "Pose should be found");
        var retrieved = found.get();

        assertEquals(
                """
                { "label": "pose-001", "sample": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [0.1, 0.2, 0.3, 0.4, 0.5] } }\
                """,
                retrieved.toString());
    }

    @Test
    void addAndFindTrajectory() {
        // Create two pose samples for the trajectory
        var sample1 = new PoseSample(Instant.EPOCH, 1.0, 2.0, 3.0, 4.0, 5.0);
        var sample2 = new PoseSample(Instant.EPOCH, 5.0, 4.0, 3.0, 2.0, 1.0);

        var trajectory = new TrajectoryRecord("traj-001", List.of(sample1, sample2));

        db.addTrajectory(trajectory);

        Optional<TrajectoryRecord> found = db.findTrajectory("traj-001");
        assertTrue(found.isPresent(), "Trajectory should be found");
        var retrieved = found.get();

        assertEquals(
"""
{
  "label": traj-001,
  "first": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [1, 2, 3, 4, 5] },
  "last": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [5, 4, 3, 2, 1] }
}
""",
                retrieved.toString());
    }

    @Test
    void sortedGettersReturnIncreasingIds() {
        // Insert three poses with decreasing ids (to force order)
        var s1 = new PoseSample(1, 2, 3, 4, 5);
        var s2 = new PoseSample(2, 2, 3, 4, 5);
        var s3 = new PoseSample(3, 2, 3, 4, 5);

        var p1 = new PoseRecord("p1", s1);
        var p2 = new PoseRecord("p2", s2);
        var p3 = new PoseRecord("p3", s3);

        db.addPose(p1);
        db.addPose(p2);
        db.addPose(p3);

        // Insert two trajectories with IDs not in order
        var t1sample = new PoseSample(1, 1, 1, 1, 1);
        var t2sample = new PoseSample(2, 2, 2, 2, 2);
        var t1 = new TrajectoryRecord("t-first", List.of(t1sample));
        var t2 = new TrajectoryRecord("t-second", List.of(t2sample));

        db.addTrajectory(t1);
        db.addTrajectory(t2);

        // Retrieve sorted lists
        List<PoseRecord> sortedPoses = db.getSortedPoses();
        assertEquals(3, sortedPoses.size());

        // IDs should be in ascending order: 1,2,3
        assertEquals("p1", sortedPoses.get(0).label());
        assertEquals("p2", sortedPoses.get(1).label());
        assertEquals("p3", sortedPoses.get(2).label());

        List<TrajectoryRecord> sortedTraj = db.getSortedTrajectories();
        assertEquals(2, sortedTraj.size());

        // The two trajectory labels should appear in ascending order
        assertEquals("t-first", sortedTraj.get(0).label());
        assertEquals("t-second", sortedTraj.get(1).label());
    }

    @Test
    void uniqueLabelsAreEnforced() {
        var s = new PoseSample(0.0, 0.0, 0.0, 0.0, 0.0);

        var poseA = new PoseRecord("unique-1", s);
        var poseB = new PoseRecord("unique-2", s);

        db.addPose(poseA);
        // Inserting a second pose with the same label should throw a RuntimeException
        var duplicatePose = new PoseRecord("unique-1", s);
        var ex =
                assertThrows(
                        RuntimeException.class,
                        () -> db.addPose(duplicatePose),
                        "Adding a pose with an existing label must fail");
        assertEquals(
                """
                pinorobotics.cogni.db.LabelExistsException: Pose with label 'unique-1' already exists\
                """,
                ex.getMessage());
    }

    @Test
    void addDeleteAddPose() {
        var poseSample = new PoseSample(Instant.EPOCH, 0.1, 0.2, 0.3, 0.4, 0.5);
        var poseRecord = new PoseRecord("pose-001", poseSample);

        db.addPose(poseRecord);
        assertEquals(true, db.findPose("pose-001").isPresent());
        db.deletePose(poseRecord.label());
        assertEquals(true, db.findPose("pose-001").isEmpty());
        db.addPose(poseRecord);
        assertEquals(true, db.findPose("pose-001").isPresent());
    }

    @Test
    void addDeleteAddTrajectory() {
        db.addTrajectory(
                new TrajectoryRecord(
                        "traj123",
                        List.of(new PoseSample(Instant.EPOCH, 0.1, 0.2, 0.3, 0.4, 0.5))));
        assertEquals(
"""
Optional[{
  "label": traj123,
  "first": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [0.1, 0.2, 0.3, 0.4, 0.5] },
  "last": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [0.1, 0.2, 0.3, 0.4, 0.5] }
}
]\
""",
                db.findTrajectory("traj123").toString());

        db.deleteTrajectory("traj123");
        assertEquals(true, db.findTrajectory("traj123").isEmpty());

        db.addTrajectory(
                new TrajectoryRecord(
                        "traj123",
                        List.of(new PoseSample(Instant.EPOCH, 0.11, 0.12, 0.13, 0.14, 0.15))));
        assertEquals(
"""
Optional[{
  "label": traj123,
  "first": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [0.11, 0.12, 0.13, 0.14, 0.15] },
  "last": { "timestamp": "1970-01-01T00:00:00Z", "jointAngles": [0.11, 0.12, 0.13, 0.14, 0.15] }
}
]\
""",
                db.findTrajectory("traj123").toString());
    }
}
