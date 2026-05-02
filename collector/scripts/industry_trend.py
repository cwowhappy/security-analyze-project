#!/usr/bin/env python3
"""
获取行业板块历史走势数据
用法：python industry_trend.py <板块名称> <period>
period: 1m, 3m, 6m, 1y
输出：JSON 数组 [{date, close, change_percent}, ...]
"""
import sys
import json
import akshare as ak
import pandas as pd


def get_industry_trend(board_name: str, period: str):
    try:
        df = ak.stock_board_industry_hist_em(
            symbol=board_name,
            period="日k",
            start_date="",
            end_date="",
            adjust=""
        )
        if df is None or df.empty:
            return []

        # 按 period 筛选数据量
        limits = {"1m": 22, "3m": 66, "6m": 132, "1y": 250}
        limit = limits.get(period, 66)
        df = df.tail(limit)

        result = []
        for _, row in df.iterrows():
            result.append({
                "date": str(row.get("日期", "")),
                "close": float(row.get("收盘", 0)) if pd.notna(row.get("收盘")) else 0,
                "change_percent": float(row.get("涨跌幅", 0)) if pd.notna(row.get("涨跌幅")) else 0,
            })
        return result
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return []


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: industry_trend.py <board_name> <period>", file=sys.stderr)
        sys.exit(1)

    board_name = sys.argv[1]
    period = sys.argv[2]
    data = get_industry_trend(board_name, period)
    print(json.dumps(data, ensure_ascii=False))
