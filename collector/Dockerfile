# syntax=docker/dockerfile:1
FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS base

WORKDIR /app

# 启用编译缓存以加速重复构建
ENV UV_COMPILE_BYTECODE=1
ENV UV_LINK_MODE=copy

# 先安装依赖（利用 Docker 层缓存）
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-install-project --no-dev

# 再复制源码并同步项目
COPY src ./src
COPY logs ./logs
RUN uv sync --frozen --no-dev

ENV PATH="/app/.venv/bin:$PATH"
ENV PYTHONPATH=/app
ENV DATABASE_URL=postgresql+psycopg://security:security@postgres:5432/security_analyze

CMD ["uv", "run", "src/main.py"]
