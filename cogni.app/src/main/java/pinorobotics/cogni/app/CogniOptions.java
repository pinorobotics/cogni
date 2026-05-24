/*
 * Copyright 2025 pinorobotics
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
package pinorobotics.cogni.app;

import java.time.Duration;

/**
 * @author aeon aeon_flux@eclipso.ch
 */
public interface CogniOptions {
    String ACTION = "action";
    String START_MCP = "startMcp";
    String R2D2_CONTROLLER = "r2d2";
    String LOG_FILE = "logFile";
    String CONTROLLER_NAME = "controllerName";
    String JOINT_STATE_TOPIC = "jointStateTopic";
    String HAND_TEACHING_CAPTURE_RATE_IN_MILLIS = "handTeachingCaptureRate";

    String DEFAULT_JOINT_STATE_TOPIC = "joint_states";
    Duration DEFAULT_HAND_TEACHING_CAPTURE_RATE_IN_MILLIS = Duration.ofMillis(500);
}
