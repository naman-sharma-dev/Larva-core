from dataclasses import dataclass


@dataclass(frozen=True)
class CommandDefinition:
    name: str
    usage: str
    description: str
    required_entities: tuple[str, ...] = ()


COMMANDS: dict[str, CommandDefinition] = {
    "help": CommandDefinition(
        name="help",
        usage="help",
        description="Show available commands.",
    ),
    "tell_time": CommandDefinition(
        name="tell_time",
        usage="time",
        description="Show current local date and time.",
    ),
    "open_app": CommandDefinition(
        name="open_app",
        usage="open <app_or_path>",
        description="Open an application, file, folder, or URL.",
        required_entities=("target",),
    ),
    "exit": CommandDefinition(
        name="exit",
        usage="exit",
        description="Quit Larva.",
    ),
}


def build_help_text() -> str:
    lines = ["Available commands:"]
    for command in COMMANDS.values():
        lines.append(f"- {command.usage}: {command.description}")
    return "\n".join(lines)