class IntentEngine:
    def classify(self, text: str, context=None):
        if "plan" in text:
            return "planning", 0.9
        if "decide" in text:
            return "decision", 0.8
        if "log" in text:
            return "reflection", 0.85
        return "informational", 0.4

    def clarification_type(self, text: str, intent_type: str):
        if intent_type == "informational":
            if "when" in text or "today" in text or "tomorrow" in text:
                return "time"
            return "goal"
        if intent_type in ["planning", "decision"]:
            return "scope"
        return "unknown"

    def clarification_prompt(self, clarification_type: str) -> str:
        prompts = {
            "goal": "What outcome are you aiming for?",
            "time": "When should this happen?",
            "scope": "What exactly do you want to work on?",
            "unknown": "Can you clarify what you need?"
        }
        return prompts.get(clarification_type, prompts["unknown"])
