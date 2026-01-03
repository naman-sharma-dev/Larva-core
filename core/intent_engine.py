class Intent:
    def __init__(
        self,
        intent_type: str,
        confidence: float,
        entities=None,
        needs_clarification=False,
        clarification_slot=None,
        clarification_prompt=None,
        session_action="one_shot"
    ):
        self.intent_type = intent_type
        self.confidence = confidence
        self.entities = entities or {}
        self.needs_clarification = needs_clarification
        self.clarification_slot = clarification_slot
        self.clarification_prompt = clarification_prompt
        self.session_action = session_action


class IntentEngine:
    def classify(self, text: str, context=None):
        text = text.lower()

        if "plan" in text:
            intent_type = "planning"
            confidence = 0.9
        elif "decide" in text:
            intent_type = "decision"
            confidence = 0.8
        elif "log" in text:
            intent_type = "reflection"
            confidence = 0.85
        else:
            intent_type = "informational"
            confidence = 0.4

        clarification_slot = self.clarification_type(text, intent_type)
        needs_clarification = clarification_slot is not None

        clarification_prompt = None
        if needs_clarification:
            clarification_prompt = self.clarification_prompt(clarification_slot)

        intent = Intent(
            intent_type=intent_type,
            confidence=confidence,
            needs_clarification=needs_clarification,
            clarification_slot=clarification_slot,
            clarification_prompt=clarification_prompt,
        )

        # 🔹 YAHAN KARNA HAI
        intent.raw_text = text

        return intent

    def clarification_type(self, text: str, intent_type: str):
        if intent_type == "informational":
            if "when" in text or "today" in text or "tomorrow" in text:
                return "time"
            return "goal"

        if intent_type in ["planning", "decision"]:
            return "scope"

        return None

    def clarification_prompt(self, clarification_type: str) -> str:
        prompts = {
            "goal": "What outcome are you aiming for?",
            "time": "When should this happen?",
            "scope": "What exactly do you want to work on?",
            "unknown": "Can you clarify what you need?",
        }
        return prompts.get(clarification_type, prompts["unknown"])
