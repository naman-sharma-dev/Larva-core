import re
from pathlib import PureWindowsPath

from core.intent import Intent


EXACT_INTENT_PHRASES: dict[str, str] = {
    "exit": "exit",
    "quit": "exit",
    "bye": "exit",
    "help": "help",
    "commands": "help",
    "what can you do": "help",
    "time": "tell_time",
    "what time is it": "tell_time",
    "current time": "tell_time",
}


def _looks_like_path(value: str) -> bool:
    lowered = value.lower()
    return (
        "\\" in value
        or "/" in value
        or ":" in value
        or lowered.startswith("http://")
        or lowered.startswith("https://")
        or "." in PureWindowsPath(value).name
    )


def _extract_open_target(text: str) -> str:
    match = re.match(r"^\s*(open|launch|start)\s+(.*)$", text, flags=re.IGNORECASE)
    if not match:
        return ""

    raw_target = match.group(2).strip()
    quoted = re.match(r'^["\'](.+)["\']$', raw_target)
    if quoted:
        raw_target = quoted.group(1).strip()

    lowered = raw_target.lower()
    for prefix in ("app ", "application ", "file ", "folder ", "the "):
        if lowered.startswith(prefix):
            raw_target = raw_target[len(prefix) :].strip()
            lowered = raw_target.lower()

    return raw_target


def _build_open_entities(target: str) -> dict[str, str]:
    entities: dict[str, str] = {"target": target}

    if _looks_like_path(target):
        entities["target_type"] = "path"
        path_obj = PureWindowsPath(target)
        file_name = path_obj.name.strip()
        if file_name:
            entities["file_name"] = file_name
        location = str(path_obj.parent).strip()
        if location and location != ".":
            entities["location"] = location
    else:
        entities["target_type"] = "app"
        entities["app_name"] = target

    return entities


def detect_intent(text: str) -> Intent:
    normalized = text.strip().lower()
    if not normalized:
        return Intent(name="unknown", confidence=0.0, raw_text=text)

    exact_match = EXACT_INTENT_PHRASES.get(normalized)
    if exact_match:
        confidence = 0.99 if exact_match in {"exit", "help"} else 0.9
        return Intent(name=exact_match, confidence=confidence, raw_text=text)

    target = _extract_open_target(text)
    if target or re.match(r"^\s*(open|launch|start)\b", text, flags=re.IGNORECASE):
        if not target:
            return Intent(
                name="open_app",
                confidence=0.75,
                raw_text=text,
                missing_entities=["target"],
                clarification_prompt="What should I open? You can give an app name or a full path.",
            )
        return Intent(
            name="open_app",
            confidence=0.95,
            raw_text=text,
            entities=_build_open_entities(target),
        )

    return Intent(name="unknown", confidence=0.4, raw_text=text)
