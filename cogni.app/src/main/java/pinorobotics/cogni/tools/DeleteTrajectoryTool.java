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
package pinorobotics.cogni.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import id.xfunction.Preconditions;
import pinorobotics.cogni.db.PoseDatabase;

/**
 * Tool for deleting a trajectory from the database.
 *
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public class DeleteTrajectoryTool {

    private final PoseDatabase poseDatabase;

    /**
     * Constructs a new DeleteTrajectoryTool with the specified pose database.
     *
     * @param poseDatabase the database to use for storing and retrieving trajectories
     */
    public DeleteTrajectoryTool(PoseDatabase poseDatabase) {
        this.poseDatabase = poseDatabase;
    }

    /**
     * Deletes a trajectory with the specified label from the database.
     *
     * @param label the label of the trajectory to delete
     * @return a message indicating whether the deletion was successful
     */
    @Tool("Delete a trajectory by its label")
    public String deleteTrajectory(@P("label to delete") String label) {
        Preconditions.notNull(label);
        poseDatabase.deleteTrajectory(label);
        return "Trajectory with label '" + label + "' has been deleted.";
    }
}
