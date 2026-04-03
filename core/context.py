from core.command_registry import COMMANDS
from core.executor import execute_intent
from core.intent import Intent
from core.intent_engine import detect_intent


_pending_clarification: dict[str, str] | None = None


def _finalize_intent(intent: Intent) -> tuple[str, bool]:
    response = execute_intent(intent)
    should_exit = intent.name == "exit"
    return response, should_exit


def process_user_input(text: str) -> tuple[str, bool]:
    global _pending_clarification

    if not text:
        return "Please type a command. Try 'help'.", False

    if _pending_clarification:
        candidate_intent = detect_intent(text)
        if candidate_intent.name != "unknown":
            _pending_clarification = None
            if candidate_intent.missing_entities:
                _pending_clarification = {
                    "intent_name": candidate_intent.name,
                    "entity": candidate_intent.missing_entities[0],
                }
                return candidate_intent.clarification_prompt or "Please provide missing details.", False
            return _finalize_intent(candidate_intent)

        clarified_intent = Intent(
            name=_pending_clarification["intent_name"],
            confidence=0.8,
            raw_text=text,
            entities={_pending_clarification["entity"]: text.strip()},
        )
        _pending_clarification = None
        return _finalize_intent(clarified_intent)

    intent = detect_intent(text)
    if intent.name in COMMANDS and intent.missing_entities:
        _pending_clarification = {
            "intent_name": intent.name,
            "entity": intent.missing_entities[0],
        }
        return intent.clarification_prompt or "Please provide missing details.", False

    return _finalize_intent(intent)