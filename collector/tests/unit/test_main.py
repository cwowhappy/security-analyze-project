"""CLI 入口单元测试。"""


class TestMain:
    """__main__.py 测试。"""

    def test_should_import_cli_main(self) -> None:
        # __main__.py 仅做入口转发，验证导入成功即可
        from data_collector.__main__ import main  # noqa: F401
        from data_collector.cli import main as cli_main  # noqa: F401
        assert True
