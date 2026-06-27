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

import dev.langchain4j.agent.tool.Tool;
import id.xfunction.XJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pinorobotics.cogni.PoseRecord;
import pinorobotics.cogni.db.PoseDatabase;

/**
 * Tool that lists all stored poses.
 *
 * <p>The result is a list of {@link PoseRecord}
 */
public class ListingTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingTool.class);
    private final PoseDatabase db;

    /**
     * Creates a new {@code ListingTool}.
     *
     * @param poseDatabase the store of pose records
     */
    public ListingTool(PoseDatabase poseDatabase) {
        this.db = poseDatabase;
    }

    @Tool("List all stored poses.")
    public String listPoses() {
        LOGGER.debug("List poses");
        return db.getSortedPoses().toString();
    }

    @Tool("List all stored trajectories.")
    public String listTrajectories() {
        LOGGER.debug("List trajectories");
        return XJson.asString("trajectories", db.getSortedTrajectories());
    }
}
