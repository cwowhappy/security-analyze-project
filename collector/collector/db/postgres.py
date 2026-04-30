import logging
import psycopg

logger = logging.getLogger(__name__)


class PostgresDB:
    def __init__(self, host: str, port: int, database: str, user: str, password: str):
        self.conninfo = (
            f"host={host} port={port} dbname={database} user={user} password={password}"
        )
        self._pool = None

    def connect(self):
        self._pool = psycopg.connect(self.conninfo)
        logger.info("PostgreSQL connected")
        return self._pool

    def get_connection(self):
        if self._pool is None or self._pool.closed:
            return self.connect()
        return self._pool

    def execute(self, sql: str, params=None):
        with self.get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                conn.commit()

    def execute_returning(self, sql: str, params=None):
        """执行 INSERT/UPDATE 并返回结果（如 RETURNING id）"""
        with self.get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                result = cur.fetchone()
                conn.commit()
                return result

    def fetchall(self, sql: str, params=None):
        with self.get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                return cur.fetchall()

    def fetchone(self, sql: str, params=None):
        with self.get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)
                return cur.fetchone()
