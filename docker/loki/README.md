# Loki Configuration

Loki is a horizontally-scalable, highly-available log aggregation system inspired by Prometheus.

## Configuration

- **Config File**: `loki-config.yml`
- **Port**: `3100`
- **Storage**: Local filesystem (`/loki`)
- **Retention**: 168 hours (7 days)

## Features

- Collects logs from all Docker containers via Promtail
- Stores logs in local filesystem
- Integrated with Grafana for log visualization
- Supports log correlation with traces via trace_id

## Usage

Logs are automatically collected by Promtail and sent to Loki. Access logs in Grafana:

1. Go to Grafana: http://localhost:3000
2. Select **Loki** data source
3. Query logs using LogQL syntax

### Example Queries

```
{container_name=~".*gateway.*"}
{container_name=~".*account.*"} |= "ERROR"
{container_name=~".*authentication.*"} | json | trace_id="abc123"
```

## Integration with Tempo

Loki is configured with derived fields to extract `trace_id` from logs and link to Tempo traces.
