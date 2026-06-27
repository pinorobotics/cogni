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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.TrajectoryRecord;
import pinorobotics.cogni.db.LabelExistsException;
import pinorobotics.cogni.db.PoseDatabase;
import pinorobotics.cogni.db.persistent.entities.PoseRecordEntity;
import pinorobotics.cogni.db.persistent.entities.TrajectoryRecordEntity;

/**
 * JPA implementation of {@link PoseDatabase} using SQLite as storage.
 *
 * <p>The class assumes the existence of the following JPA entity classes (they must be provided
 * elsewhere in the project):
 *
 * <ul>
 *   <li>{@code PoseSampleEntity} – stores the five joint angles and a timestamp.
 *   <li>{@code PoseRecordEntity} – stores a label and a reference to a {@code PoseSampleEntity}.
 *   <li>{@code TrajectoryRecordEntity} – stores a trajectory label and a collection of {@code
 *       TrajectorySampleEntity}s.
 *   <li>{@code TrajectorySampleEntity} – stores a single pose that belongs to a {@code
 *       TrajectoryRecordEntity}.
 * </ul>
 *
 * All these entities must map to tables {@code pose_samples}, {@code pose_records}, {@code
 * trajectory_records} and {@code trajectory_samples} respectively.
 *
 * @author aeon aeon_flux@eclipso.ch
 */
public class SqlitePoseDatabase implements PoseDatabase {

    private EntityManagerFactory emf;
    private EntityTransformer transformer = new EntityTransformer();

    /**
     * Creates a new SQLite based {@code PoseDatabase}.
     *
     * @param dbPath the path of the SQLite database file (e.g. "cogni.db")
     */
    public SqlitePoseDatabase(Path dbPath) {
        Map<String, Object> props =
                Map.of(
                        "javax.persistence.jdbc.url",
                        "jdbc:sqlite:" + dbPath.toAbsolutePath(),
                        "javax.persistence.jdbc.driver",
                        "org.sqlite.JDBC",
                        "hibernate.show_sql",
                        "false",
                        "jakarta.persistence.schema-generation.database.action",
                        "create",
                        "hibernate.dialect",
                        "org.hibernate.community.dialect.SQLiteDialect");
        this.emf = Persistence.createEntityManagerFactory("cogniPU", props);
    }

    @Override
    public void addPose(PoseRecord pose) {
        EntityManager em = emf.createEntityManager();
        try {
            var tx = em.getTransaction();
            tx.begin();
            // Check if pose with same label already exists
            if (findPose(pose.label()).isPresent()) {
                throw new LabelExistsException(
                        "Pose with label '" + pose.label() + "' already exists");
            }
            em.persist(transformer.toPoseRecordEntity(pose));
            tx.commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    @Override
    public void addTrajectory(TrajectoryRecord trajectory) {
        EntityManager em = emf.createEntityManager();
        try {
            // Check if trajectory with same label already exists
            if (findTrajectory(trajectory.label()).isPresent()) {
                throw new LabelExistsException(
                        "Trajectory with label '" + trajectory.label() + "' already exists");
            }
            em.getTransaction().begin();
            em.persist(transformer.toTrajectoryRecordEntity(trajectory));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<PoseRecord> findPose(String label) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM PoseRecordEntity t WHERE t.label = :label",
                            PoseRecordEntity.class)
                    .setParameter("label", label)
                    .getResultStream()
                    .findFirst()
                    .map(transformer::toPoseRecord);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<TrajectoryRecord> findTrajectory(String label) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM TrajectoryRecordEntity t WHERE t.label = :label",
                            TrajectoryRecordEntity.class)
                    .setParameter("label", label)
                    .getResultStream()
                    .findFirst()
                    .map(transformer::toTrajectoryRecord);
        } catch (NoResultException nre) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    @Override
    public List<PoseRecord> getSortedPoses() {
        EntityManager em = emf.createEntityManager();
        try {
            return em
                    .createQuery(
                            "SELECT p FROM PoseRecordEntity p ORDER BY p.id",
                            PoseRecordEntity.class)
                    .getResultList()
                    .stream()
                    .map(transformer::toPoseRecord)
                    .toList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<TrajectoryRecord> getSortedTrajectories() {
        EntityManager em = emf.createEntityManager();
        try {
            return em
                    .createQuery(
                            "SELECT t FROM TrajectoryRecordEntity t ORDER BY t.id",
                            TrajectoryRecordEntity.class)
                    .getResultList()
                    .stream()
                    .map(transformer::toTrajectoryRecord)
                    .toList();
        } finally {
            em.close();
        }
    }

    @Override
    public void deletePose(String label) {
        EntityManager em = emf.createEntityManager();
        try {
            var tx = em.getTransaction();
            tx.begin();
            em.createQuery(
                            "SELECT t FROM PoseRecordEntity t WHERE t.label = :label",
                            PoseRecordEntity.class)
                    .setParameter("label", label)
                    .getResultStream()
                    .findFirst()
                    .ifPresent(em::remove);
            tx.commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteTrajectory(String label) {
        EntityManager em = emf.createEntityManager();
        try {
            var tx = em.getTransaction();
            tx.begin();
            em.createQuery(
                            "SELECT t FROM TrajectoryRecordEntity t WHERE t.label = :label",
                            TrajectoryRecordEntity.class)
                    .setParameter("label", label)
                    .getResultStream()
                    .findFirst()
                    .ifPresent(em::remove);
            tx.commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    /** Releases the JPA {@code EntityManagerFactory}. Call this when the application shuts down. */
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
