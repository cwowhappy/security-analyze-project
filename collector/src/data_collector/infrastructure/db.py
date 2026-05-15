"""PostgreSQL 数据库连接池与原始访问层。"""

from contextlib import contextmanager
from typing import Any

import psycopg2
import psycopg2.extras
import structlog
from psycopg2.pool import ThreadedConnectionPool

from data_collector.config import Settings

logger = structlog.get_logger(__name__)

_pool: ThreadedConnectionPool | None = None


def init_pool(settings: Settings) -> ThreadedConnectionPool:
    """初始化数据库连接池。"""
    global _pool
    if _pool is not None:
        return _pool

    logger.info(
        "初始化数据库连接池",
        host=settings.db_host,
        port=settings.db_port,
        database=settings.db_name,
        minconn=settings.db_pool_min_size,
        maxconn=settings.db_pool_max_size,
    )

    _pool = ThreadedConnectionPool(
        minconn=settings.db_pool_min_size,
        maxconn=settings.db_pool_max_size,
        host=settings.db_host,
        port=settings.db_port,
        database=settings.db_name,
        user=settings.db_user,
        password=settings.db_password,
    )
    return _pool


def get_pool() -> ThreadedConnectionPool:
    """获取当前连接池，未初始化时抛出 RuntimeError。"""
    if _pool is None:
        raise RuntimeError("数据库连接池未初始化，请先调用 init_pool()")
    return _pool


@contextmanager
def get_connection():
    """获取数据库连接的上下文管理器。"""
    conn = get_pool().getconn()
    try:
        yield conn
    finally:
        get_pool().putconn(conn)


@contextmanager
def get_cursor(cursor_factory=None):
    """获取数据库游标的上下文管理器。"""
    with get_connection() as conn:
        cursor = conn.cursor(cursor_factory=cursor_factory)
        try:
            yield cursor
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            cursor.close()


def execute_query(sql: str, params: tuple[Any, ...] | None = None) -> list[dict[str, Any]]:
    """执行查询语句并返回字典列表。"""
    with get_cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cursor:
        cursor.execute(sql, params)
        return [dict(row) for row in cursor.fetchall()]


def execute_update(sql: str, params: tuple[Any, ...] | None = None) -> int:
    """执行更新语句并返回影响行数。"""
    with get_cursor() as cursor:
        cursor.execute(sql, params)
        return cursor.rowcount


@contextmanager
def transaction():
    """显式事务控制的上下文管理器。

    适用于需要批量执行多条 SQL 并统一提交的场景。
    使用方式::

        with transaction() as conn:
            cursor = conn.cursor()
            cursor.execute(...)
            cursor.execute(...)
            cursor.close()
    """
    conn = get_pool().getconn()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        get_pool().putconn(conn)


def close_pool() -> None:
    """关闭连接池。"""
    global _pool
    if _pool is not None:
        _pool.closeall()
        _pool = None
        logger.info("数据库连接池已关闭")
