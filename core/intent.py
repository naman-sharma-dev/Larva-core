from dataclasses import dataclass, field


@dataclass
class Intent:
    name: str
    confidence: float
    raw_text: str
    entities: dict[str, str] = field(default_factory=dict)
    missing_entities: list[str] = field(default_factory=list)
    clarification_prompt: str | None = None
