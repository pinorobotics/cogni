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
 * Tool for deleting a pose from the database.
 *
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public class DeletePoseTool {

    private final PoseDatabase db;

    /**
     * Creates a new DeletePoseTool instance.
     *
     * @param db the pose database to use
     */
    public DeletePoseTool(PoseDatabase db) {
        this.db = db;
    }

    /**
     * Deletes a pose with the given label from the database.
     *
     * @param label the label of the pose to delete
     * @return a message indicating whether the deletion was successful
     */
    @Tool("Deletes a pose with the given label from the database")
    public String deletePose(@P("label to delete") String label) {
        Preconditions.notNull(label);
        db.deletePose(label);
        return "Pose with label '" + label + "' deleted successfully.";
    }
}
