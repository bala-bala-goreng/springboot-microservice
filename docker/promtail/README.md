# Promtail Configuration

Promtail is an agent that ships the contents of local logs to a private Grafana Loki instance.

## Configuration

- **Config File**: `promtail.yml`
- **Port**: `9080` (internal)
- **Log Source**: Docker container logs (`/var/lib/docker/containers/*/*-json.log`)

## Features

- Automatically discovers and collects logs from all Docker containers
- Parses JSON log format from Docker
- Extracts container names and metadata
- Sends logs to Loki for aggregation

## Log Collection

Promtail reads logs from:
- `/var/lib/docker/containers/*/*-json.log` - Docker container log files

## Pipeline Stages

1. **JSON Parsing**: Extracts log output, stream, and attributes
2. **Container Metadata**: Extracts container name from Docker tags
3. **Label Extraction**: Adds container_name and stream as labels
4. **Output**: Sends processed logs to Loki

## Labels

Each log entry includes:
- `container_name`: Docker container name
- `stream`: stdout or stderr
- `job`: Always "docker"
