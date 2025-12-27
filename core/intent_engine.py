class IntentEngine:
    def classify(self, text: str, context=None) -> str:
        if "plan" in text:
            return "planning"
        if "decide" in text:
            return "decision"
        if "log" in text:
            return "reflection"
        return "informational"
