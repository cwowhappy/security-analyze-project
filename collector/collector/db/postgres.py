import logging
from contextlib import contextmanager
from typing import Optional, List, Any, Iterator

import psycopg
from psycopg_pool import ConnectionPool

logger = logging.getLogger(__name__)


class PostgresDB:
    """PostgreSQL 数据库封装（基于 psycopg_pool ConnectionPool）

    支持上下文管理器、显式事务控制、批量写入。
    """

    def __init__(
        self,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        pool_min_size: int = 1,
        pool_max_size: int = 5,
        pool_max_idle: float = 300.0,
        pool_max_lifetime: float = 3600.0,
    ):
        conninfo = f"host={host} port={port} dbname={database} user={user} password={password}"
        self._pool = ConnectionPool(
            conninfo,
            min_size=pool_min_size,
            max_size=pool_max_size,
            max_idle=pool_max_idle,
            max_lifetime=pool_max_lifetime,
            kwargs={"autocommit": False},
            open=True,
        )
        logger.info(
            f"PostgreSQL pool created (min={pool_min_size}, max={pool_max_size})"
        )

    # ------------------------------------------------------------------
    # 上下文管理器
    # ------------------------------------------------------------------
    @contextmanager
    def connection(self) -> Iterator[psycopg.Connection]:
        """从连接池借出连接，使用后自动归还。"""
        with self._pool.connection() as conn:
            yield conn

    @contextmanager
    def transaction(self) -> Iterator[psycopg.Cursor]:
        """在借出的连接上执行事务，自动提交或回滚。"""
        with self.connection() as conn:
            with conn.cursor() as cur:
                try:
                    yield cur
                    conn.commit()
                except Exception:
                    conn.rollback()
                    raise

    def close(self):
        """关闭整个连接池。"""
        if self._pool:
            self._pool.close()
            logger.info("PostgreSQL pool closed")

    def __enter__(self) -> "PostgresDB":
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    # ------------------------------------------------------------------
    # 基础执行
    # ------------------------------------------------------------------
    def execute(self, sql: str, params=None) -> None:
        """执行写操作并自动提交。"""
        with self.transaction() as cur:
            cur.execute(sql, params)

    def execute_returning(self, sql: str, params=None) -> Optional[tuple]:
        """执行 INSERT/UPDATE 并返回结果（如 RETURNING id）。"""
        with self.transaction() as cur:
            cur.execute(sql, params)
            return cur.fetchone()

    def fetchall(self, sql: str, params=None) -> List[tuple]:
        """执行查询并返回全部结果。"""
        with self.connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                return cur.fetchall()

    def fetchone(self, sql: str, params=None) -> Optional[tuple]:
        """执行查询并返回单行结果。"""
        with self.connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                return cur.fetchone()

    # ------------------------------------------------------------------
    # 批量写入
    # ------------------------------------------------------------------
    def insert_many(self, sql: str, params_seq: List[tuple]) -> int:
        """使用 executemany 批量插入/更新，自动提交。

        Args:
            sql: 参数化 SQL（使用 %s 占位符）。
            params_seq: 参数元组列表。
        """
        if not params_seq:
            return 0
        with self.transaction() as cur:
            cur.executemany(sql, params_seq)
            return cur.rowcount

    def upsert_many(self, sql: str, params_seq: List[tuple]) -> int:
        """批量执行带 ON CONFLICT 的 upsert SQL，自动提交。

        Args:
            sql: 带 ON CONFLICT DO UPDATE 的参数化 INSERT SQL。
            params_seq: 参数元组列表。
        """
        if not params_seq:
            return 0
        with self.transaction() as cur:
            cur.executemany(sql, params_seq)
            return cur.rowcount
