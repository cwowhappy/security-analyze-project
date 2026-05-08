"""公共工具函数

提取各模块重复的解析、推断逻辑，避免代码冗余。
"""
import re
import logging
from typing import Optional
from datetime import datetime

logger = logging.getLogger(__name__)


def infer_market(stock_code: str) -> Optional[str]:
    """根据股票代码推断市场板块"""
    if not stock_code:
        return None
    code = str(stock_code).strip()
    if len(code) != 6:
        return "HK" if len(code) == 5 else None
    first = code[0]
    if first in ("6", "9"):
        return "SH"
    elif first in ("0", "2", "3"):
        return "SZ"
    elif first in ("4", "8"):
        return "BJ"
    return None


def parse_date(date_str) -> Optional[str]:
    """解析日期字符串为 YYYY-MM-DD

    支持格式：YYYY-MM-DD、YYYYMMDD、YYYY/MM/DD、datetime 对象。
    """
    if not date_str:
        return None
    # datetime 对象直接格式化
    if hasattr(date_str, "strftime"):
        try:
            return date_str.strftime("%Y-%m-%d")
        except (ValueError, TypeError):
            return None
    try:
        s = str(date_str).strip()
        if " " in s:
            s = s.split(" ")[0]
        for fmt in ("%Y-%m-%d", "%Y%m%d", "%Y/%m/%d"):
            try:
                dt = datetime.strptime(s, fmt)
                return dt.strftime("%Y-%m-%d")
            except ValueError:
                continue
        return None
    except (ValueError, TypeError):
        return None


def parse_capital(capital) -> Optional[float]:
    """解析注册资本/注册资金为数字（万元）"""
    if capital is None:
        return None
    match = re.search(r"([\d,.]+)", str(capital))
    if match:
        try:
            return float(match.group(1).replace(",", ""))
        except (ValueError, TypeError):
            return None
    return None


def extract_region(address: str) -> Optional[str]:
    """从注册地址提取省份/城市"""
    if not address:
        return None
    match = re.search(r"^(.*?省|.*?自治区|北京|天津|上海|重庆)", address)
    if match:
        return match.group(1)
    return None
