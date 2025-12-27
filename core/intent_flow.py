from dataclasses import dataclass
from typing import Dict, Optional
from core.intent_engine import IntentEngine
from core.memory import Memory


@dataclass
class Intent:
    intent_type: str
    entities: Dict[str, str]
    confidence: float
    session_action: str
    needs_clarification: bool
    clarification_type: Optional[str]


class IntentFlow:
    def __init__(self, memory=None, session_manager=None):
        self.memory = memory or Memory()
        self.session_manager = session_manager
        self.engine = IntentEngine()

    def process_input(self, raw_input: str) -> Intent:
        cleaned_input = self._preprocess(raw_input)
        context = self._fetch_context()
        intent_type, confidence = self.engine.classify(cleaned_input, context)
        entities = self._extract_entities(cleaned_input)
        normalized_intent = self._normalize(intent_type, entities)
        session_action = self._decide_session(normalized_intent)

        needs_clarification = confidence < 0.5
        clarification_type = None

        if normalized_intent in ["planning", "decision"] and not entities:
            needs_clarification = True
            clarification_type = "scope"
        elif needs_clarification:
            clarification_type = self.engine.clarification_type(
                cleaned_input, normalized_intent
            )

        return Intent(
            intent_type=normalized_intent,
            entities=entities,
            confidence=confidence,
            session_action=session_action,
            needs_clarification=needs_clarification,
            clarification_type=clarification_type
        )

    def _preprocess(self, text: str) -> str:
        return text.strip().lower()

    def _fetch_context(self) -> Optional[dict]:
        return self.memory.get_context()

    def _extract_entities(self, text: str) -> Dict[str, str]:
        entities = {}
        if "tomorrow" in text:
            entities["time"] = "tomorrow"
        if "today" in text:
            entities["time"] = "today"
        return entities

    def _normalize(self, intent_type: str, entities: Dict[str, str]) -> str:
        return intent_type

    def _decide_session(self, intent_type: str) -> str:
        if intent_type in ["planning", "decision"]:
            return "start_or_continue"
        return "one_shot"
