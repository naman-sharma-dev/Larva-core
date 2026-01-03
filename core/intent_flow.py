from core.memory import ShortTermMemory


REQUIRED_SLOTS = {
    "planning": ["goal", "time"],
    "decision": ["goal"],
    "informational": ["goal"],
}

CORRECTION_KEYWORDS = ["actually", "change", "instead", "make it"]
TIME_KEYWORDS = ["am", "pm", "today", "tomorrow"]


class IntentFlow:
    def __init__(self, memory: ShortTermMemory):
        self.memory = memory

    def process(self, intent):
        # 0️⃣ Conflict confirmation has highest priority
        if hasattr(intent, "pending_conflict"):
            intent.needs_clarification = True
            intent.clarification_prompt = (
                f'Earlier you said "{intent.pending_conflict["old"]}". '
                f'Do you want to change it to "{intent.pending_conflict["new"]}"? (yes/no)'
            )
            return intent

        # 1️⃣ Slot update detection (ONLY once per intent)
        if not hasattr(intent, "_slot_update_checked"):
            intent._slot_update_checked = True

            if self._looks_like_slot_update(intent):
                slot, value = self._extract_slot_update(intent)
                if slot:
                    self.fill_slot(intent, slot, value, source="user")
                    return self.process(intent)

        # 2️⃣ Normal slot completion
        missing_slot = self._get_next_missing_slot(intent)

        if not missing_slot:
            intent.needs_clarification = False
            intent.clarification_slot = None
            intent.clarification_prompt = None
            return intent

        # Try filling from memory
        if self.memory.has_slot(missing_slot):
            value = self.memory.get_slot(missing_slot)
            self.fill_slot(intent, missing_slot, value, source="memory")
            return self.process(intent)

        # Ask user
        intent.needs_clarification = True
        intent.clarification_slot = missing_slot
        intent.clarification_prompt = self._clarification_prompt(missing_slot)
        return intent

    def handle_clarification_answer(self, intent, answer: str):
        answer = answer.strip().lower()

        # Conflict confirmation
        if hasattr(intent, "pending_conflict"):
            if answer in ["yes", "y"]:
                slot = intent.pending_conflict["slot"]
                new_value = intent.pending_conflict["new"]
                self.fill_slot(
                    intent,
                    slot,
                    new_value,
                    source="user",
                    overwrite=True
                )

            del intent.pending_conflict
            return self.process(intent)

        # Normal clarification answer
        slot = intent.clarification_slot
        self.fill_slot(intent, slot, answer, source="user")
        return self.process(intent)

    def fill_slot(self, intent, slot: str, value: str, source="user", overwrite=False):
        # 🔴 MEMORY vs USER conflict detection
        if (
            source == "user"
            and not overwrite
            and self.memory.has_slot(slot)
            and self.memory.get_slot(slot) != value
        ):
            intent.pending_conflict = {
                "slot": slot,
                "old": self.memory.get_slot(slot),
                "new": value,
            }
            return

        intent.entities[slot] = {
            "value": value,
            "source": source,
        }

        # Promotion logic
        if intent.intent_type == "informational" and slot == "goal":
            intent.intent_type = "planning"

    # ---------- helpers ----------

    def _get_next_missing_slot(self, intent):
        required = REQUIRED_SLOTS.get(intent.intent_type, [])
        for slot in required:
            if slot not in intent.entities:
                return slot
        return None

    def _clarification_prompt(self, slot: str):
        prompts = {
            "goal": "What outcome are you aiming for?",
            "time": "When should this happen?",
            "scope": "What exactly do you want to work on?",
        }
        return prompts.get(slot, "Can you clarify what you need?")

    def _looks_like_slot_update(self, intent):
        text = getattr(intent, "raw_text", "").lower()
        return (
            any(k in text for k in CORRECTION_KEYWORDS)
            and self.memory.has_slot("time")
        )

    def _extract_slot_update(self, intent):
        text = intent.raw_text.lower()

        if any(k in text for k in TIME_KEYWORDS):
            return "time", intent.raw_text

        return None, None
