# Python Sidecar Internalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Internalize the Python sidecar into `cloudys` without changing the Java HTTP contract.

**Architecture:** Keep the sidecar's existing root layout under `services/python-sidecar`, preserve `sidecar_app:app` as the compatibility entrypoint, and move all local/dev/container build flows to repo-local `uv`-managed execution.

**Tech Stack:** FastAPI, uv, pytest, Spring Boot, Docker Compose, Jenkins

---
