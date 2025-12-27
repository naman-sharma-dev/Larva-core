class IntentEngine:
    def classify(self, text: str, context=None):
        if "plan" in text:
            return "planning", 0.9
        if "decide" in text:
            return "decision", 0.8
        if "log" in text:
            return "reflection", 0.85
        return "informational", 0.4
