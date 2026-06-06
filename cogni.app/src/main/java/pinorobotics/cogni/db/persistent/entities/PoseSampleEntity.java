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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * @author aeon aeon_flux@eclipso.ch
 */
@Entity
@Table(name = "pose_samples")
public class PoseSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double joint1;
    private double joint2;
    private double joint3;
    private double joint4;
    private double joint5;

    @Column(name = "timestamp")
    private Instant timestamp;

    public PoseSampleEntity() {}

    public PoseSampleEntity(
            double joint1,
            double joint2,
            double joint3,
            double joint4,
            double joint5,
            Instant timestamp) {
        this.joint1 = joint1;
        this.joint2 = joint2;
        this.joint3 = joint3;
        this.joint4 = joint4;
        this.joint5 = joint5;
        this.timestamp = timestamp;
    }

    // getters and setters

    public Long getId() {
        return id;
    }

    public double getJoint1() {
        return joint1;
    }

    public void setJoint1(double joint1) {
        this.joint1 = joint1;
    }

    public double getJoint2() {
        return joint2;
    }

    public void setJoint2(double joint2) {
        this.joint2 = joint2;
    }

    public double getJoint3() {
        return joint3;
    }

    public void setJoint3(double joint3) {
        this.joint3 = joint3;
    }

    public double getJoint4() {
        return joint4;
    }

    public void setJoint4(double joint4) {
        this.joint4 = joint4;
    }

    public double getJoint5() {
        return joint5;
    }

    public void setJoint5(double joint5) {
        this.joint5 = joint5;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
