# Python Sidecar Internalization Design

Goal: move the Python sidecar from `../Semantic-Atlas/backend_inference` into `cloudys/services/python-sidecar` while keeping the Java HTTP contract stable.

Key decisions:

- Keep the upstream root layout under `services/python-sidecar/`.
- Keep `sidecar_app:app` as the Java-facing default module.
- Use `uv` as the dependency and execution entrypoint.
- Commit `uv.lock`.
- Migrate the persisted KB index assets into the repo.
