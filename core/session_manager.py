class SessionManager:
    def __init__(self):
        self.active_session = None
        self.counter = 0

    def start_or_continue(self, intent):
        return self._start_or_continue(intent)

    def _start_or_continue(self, intent):
        if intent.session_action == "one_shot":
            return "no_session"

        if self.active_session is None:
            self.counter += 1
            self.active_session = f"session_{self.counter:03d}"
            return self.active_session

        return self.active_session

    def reset(self):
        self.active_session = None
