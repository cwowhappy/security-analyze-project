"""FastAPI HTTP API：暴露 REST 接口供后端调用。"""

from typing import Any

import structlog
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from data_collector.adapters.akshare_source import AkshareDataSource
from data_collector.adapters.db_collection_task_repository import DbCollectionTaskRepository
from data_collector.adapters.db_company_repository import DbCompanyRepository
from data_collector.adapters.db_stock_repository import DbStockRepository
from data_collector.adapters.tushare_source import TushareDataSource
from data_collector.config import get_settings
from data_collector.core.ports.data_source import DataSource
from data_collector.infrastructure.db import close_pool, execute_query, init_pool
from data_collector.scheduler import CollectionScheduler
from data_collector.task_executor import TaskExecutor

logger = structlog.get_logger(__name__)

app = FastAPI(
    title="Stock Collector API",
    description="股票数据采集器 HTTP API",
    version="0.1.0",
)

# 全局状态
_scheduler: CollectionScheduler | None = None
_sources: list[DataSource] = []


class CreateTaskRequest(BaseModel):
    """创建即时任务请求。"""

    task_type: str = Field(..., description="任务类型: stock_full/company_full/stock_single/company_single")
    task_params: dict = Field(default_factory=dict, description="任务参数")
    data_source: str | None = Field(None, description="指定数据源: akshare/tushare")


class TaskResponse(BaseModel):
    """任务响应。"""

    id: str
    task_type: str
    status: str
    data_source: str | None
    total_count: int
    success_count: int
    fail_count: int
    error_message: str | None
    started_at: str | None
    completed_at: str | None


class HealthResponse(BaseModel):
    """健康检查响应。"""

    status: str
    sources: list[dict[str, Any]]
    db_connected: bool


@app.on_event("startup")
def startup() -> None:
    """应用启动时初始化数据库连接池和调度器。"""
    global _scheduler, _sources

    settings = get_settings()
    logger.info("API 启动中", log_level=settings.log_level)

    # 初始化数据库连接池
    init_pool(settings)

    # 初始化数据源
    _sources = [AkshareDataSource(settings), TushareDataSource(settings)]

    # 初始化仓库
    stock_repo = DbStockRepository()
    company_repo = DbCompanyRepository()
    task_repo = DbCollectionTaskRepository()

    # 初始化任务执行器
    executor = TaskExecutor(
        sources=_sources,
        stock_repo=stock_repo,
        company_repo=company_repo,
        settings=settings,
    )

    # 初始化调度器
    _scheduler = CollectionScheduler(
        executor=executor,
        task_repo=task_repo,
        settings=settings,
    )
    _scheduler.start()

    logger.info("API 启动完成")


@app.on_event("shutdown")
def shutdown() -> None:
    """应用关闭时清理资源。"""
    global _scheduler
    if _scheduler:
        _scheduler.shutdown(wait=False)
    close_pool()
    logger.info("API 已关闭")


@app.post("/tasks", response_model=dict)
def create_task(req: CreateTaskRequest) -> dict:
    """创建即时采集任务。

    APScheduler 会立即调度执行。
    """
    if _scheduler is None:
        raise HTTPException(status_code=503, detail="调度器未初始化")

    task_id = _scheduler.add_instant_task(
        task_type=req.task_type,
        task_params=req.task_params,
        data_source=req.data_source,
    )
    return {"id": task_id, "status": "submitted"}


@app.get("/tasks")
def list_tasks(limit: int = 100) -> list[TaskResponse]:
    """查询最近的任务执行历史。"""
    repo = DbCollectionTaskRepository()
    tasks = repo.find_all(limit=limit)
    return [
        TaskResponse(
            id=t.id or "",
            task_type=t.task_type,
            status=t.status,
            data_source=t.data_source,
            total_count=t.total_count,
            success_count=t.success_count,
            fail_count=t.fail_count,
            error_message=t.error_message,
            started_at=t.started_at.isoformat() if t.started_at else None,
            completed_at=t.completed_at.isoformat() if t.completed_at else None,
        )
        for t in tasks
    ]


@app.get("/tasks/{task_id}")
def get_task(task_id: str) -> TaskResponse:
    """查询单条任务详情。"""
    repo = DbCollectionTaskRepository()
    task = repo.find_by_id(task_id)
    if not task:
        raise HTTPException(status_code=404, detail="任务不存在")
    return TaskResponse(
        id=task.id or "",
        task_type=task.task_type,
        status=task.status,
        data_source=task.data_source,
        total_count=task.total_count,
        success_count=task.success_count,
        fail_count=task.fail_count,
        error_message=task.error_message,
        started_at=task.started_at.isoformat() if task.started_at else None,
        completed_at=task.completed_at.isoformat() if task.completed_at else None,
    )


@app.get("/health", response_model=HealthResponse)
def health_check() -> HealthResponse:
    """采集器健康检查（数据源连通性）。"""
    sources_health = []
    for s in _sources:
        try:
            health = s.check_health()
            sources_health.append({
                "name": s.name,
                "available": s.is_available(),
                "status": health.status.value,
                "latency_ms": health.latency_ms,
                "error_rate": health.error_rate,
            })
        except Exception as e:
            sources_health.append({
                "name": s.name,
                "available": False,
                "status": "error",
                "error": str(e),
            })

    # 简单的 DB 连通性检查
    db_connected = False
    try:
        execute_query("SELECT 1")
        db_connected = True
    except Exception:
        pass

    all_available = all(s.get("available", False) for s in sources_health)
    status = "healthy" if all_available and db_connected else "degraded"

    return HealthResponse(
        status=status,
        sources=sources_health,
        db_connected=db_connected,
    )
