Cogni – AI‑Powered Robotic Arm Assistant.

Cogni is a **Java‑based AI agent** that sits on top of a ROS2‑controlled 5-DoF robotic arm.
It lets you:

| Capability | Command example |
|------------|--------------------------|
| **Label a pose** – store the current arm configuration under a human‑readable name (e.g., `light switch ON`). | "Assign *light switch ON* to the current pose." |
| **Recall a pose** – move the arm to a previously saved label. | “Move to *spoon pose*.” |
| **List all stored labels**. | “Show me all poses.” |
| **Chain movements** – travel from one labelled pose to another. | “Light switch ON, light switch OFF.” |

All of this is exposed through a [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that can be launched from the command line. External programs (or other AI agents) can use the same functionality programmatically via MCP "stdio" interface.

# Prerequisites

| Item | Minimum version / requirement |
|------|------------------------------|
| **ROS2** | Humble or Foxy |
| **5 DoF robotic arm**| Configured with ROS2 [joint_trajectory_controller](https://control.ros.org/jazzy/doc/ros2_controllers/joint_trajectory_controller/doc/userdoc.html)
| **Java** | 25 (or later) |
| **Operating System** | Linux (Ubuntu 22.04 recommended) – any OS that can run Java + ROS 2 |

# Dependencies

| Item | Minimum version / requirement |
|------|------------------------------|
| [jros2client](https://github.com/lambdaprime/jros2client)| Latest version - handles ROS2 communication |
| [langchain4j](https://docs.langchain4j.dev) | Latest stable release – provides stdio MCP server support |

# Installing Cogni

Download the latest [release version](cogni.app/release/CHANGELOG.md) and extract to a local folder.

# Understanding the CLI Options

Required options:

| Option | Description |
|--------|-------------|
| -action | Must be **~startMcp~** (the only supported action for now).|
| -controllerName | Name of the ROS2 `joint_trajectory_controller`. |

Non-required:

| Option | Description | Default |
|--------|-------------|---------|
| -logFile | Path to output log file. | Logs are sent to stdout. |
| -jointStateTopic | ROS2 topic to subscribe to for joint states. | `/joint_states` |
| -h | Show help and exit. | X |

When Cogni starts it discovers ROS2 `joint_trajectory_controller` Action server where it will submit  movement commands and separately it subscribes to the selected joint states topic (eg. `/joint_states`).

# Using Cogni – Step-by-Step Tutorial

## Register Cogni MCP with your agent

Most AI Agents (OpenClaw, Cursor, Codex, Claude, ...) support MCP registration via a [mcp.json](https://modelcontextprotocol.io/docs/develop/connect-local-servers).

Below is an example for Claude:

```json
{
  "mcpServers": {
    "cogni": {
      "command": "cogni",
      "args": [
        "-action=startMcp", "-logFile=/tmp/cogni.log", "-controllerName=dorna2_arm_controller"
      ]
    }
  }
}
```

## Start agent with Cogni instructions

Cogni instruction can be found inside the installation folder `agent/AGENTS.md`

## Labeling and moving the arm

Assuming that initially arm is in home position we can ask agent to save it under "home" label.

For other poses use your robot’s **teach pendant** or **ROS MoveIt** to manually position the arm and then ask agent to assign a label for it.

### Recall a pose
```
"Move to 'spoon pose'."
```
**Response:**
```
Arm moved to 'spoon pose'.
```

### Chain movements
```
"Light switch ON, light switch OFF, home"
```
**Response:**
```
Moving to light switch ON, light switch OFF, home...
Done!
```

### List all saved labels
```
"Show me all poses."
```
**Response:**
```
Saved poses:
1. home
2. light switch ON
3. light switch OFF
```

