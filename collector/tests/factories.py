import factory
from src.models import SecurityEvent


class SecurityEventFactory(factory.Factory):
    class Meta:
        model = SecurityEvent

    title = factory.Sequence(lambda n: f"示例安全事件 #{n}")
    description = factory.Faker("sentence")
    severity = "MEDIUM"
    source = factory.Faker("company")
    event_type = "TEST"
    raw_data = {"test": True}
