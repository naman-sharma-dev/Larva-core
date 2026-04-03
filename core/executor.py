from datetime import datetime
import subprocess

from core.command_registry import build_help_text
from core.intent import Intent


def _handle_help(_: Intent) -> str:
    return build_help_text()


def _handle_tell_time(_: Intent) -> str:
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    return f"Current time: {now}"


def _handle_open(intent: Intent) -> str:
    target = (
        intent.entities.get("target", "").strip()
        or intent.entities.get("app_name", "").strip()
    )
    if not target:
        return "Please provide an app or path to open."

    try:
        subprocess.Popen(["cmd", "/c", "start", "", target])
        return f"Opening: {target}"
    except Exception as exc:
        return f"Could not open '{target}': {exc}"


def _handle_exit(_: Intent) -> str:
    return "Larva shutting down."


HANDLERS = {
    "help": _handle_help,
    "tell_time": _handle_tell_time,
    "open_app": _handle_open,
    "exit": _handle_exit,
}


def execute_intent(intent: Intent) -> str:
    handler = HANDLERS.get(intent.name)
    if handler:
        return handler(intent)

    return (
        "I did not understand that yet. Try 'help'. "
        "(Phase 1 uses simple rule-based intent detection.)"
    )
