class Executor:
    def execute(self, intent):
        if intent.intent_type == "planning":
            return self._execute_planning(intent)

        return "I am not sure how to act on this yet."

    def _execute_planning(self, intent):
        goal = intent.entities.get("goal", {}).get("value")
        time = intent.entities.get("time", {}).get("value")

        if goal and time:
            return f"Got it. You want to work on {goal} at {time}."

        if goal:
            return f"Got it. You want to work on {goal}."

        return "Got it."
