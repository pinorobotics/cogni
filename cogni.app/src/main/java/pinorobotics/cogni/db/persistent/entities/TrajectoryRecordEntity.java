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
package pinorobotics.cogni.db.persistent.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity representing a stored trajectory.
 *
 * @author aeon aeon_flux@eclipso.ch
 */
@Entity
@Table(name = "trajectory_records")
public class TrajectoryRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label", nullable = false, unique = true)
    private String label;

    public TrajectoryRecordEntity() {}

    /**
     * One-to-many relationship: a trajectory record consists of several sample entities. The {@code
     * mappedBy} side of {@code TrajectorySampleEntity.trajectoryRecord} is omitted because we store
     * the back‑reference here for convenience. The owning side is defined in {@code
     * TrajectorySampleEntity} via {@code @ManyToOne(...)}.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PoseSampleEntity> samples = new ArrayList<>();

    public TrajectoryRecordEntity(String label, List<PoseSampleEntity> samples) {
        this.label = label;
        this.samples = samples;
    }

    // getters and setters

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<PoseSampleEntity> getSamples() {
        return samples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrajectoryRecordEntity that = (TrajectoryRecordEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }
}
