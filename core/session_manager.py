class SessionManager:
    def __init__(self):
        self.active_session = None

    def handle(self, session_action: str) -> str:
        if session_action == "start_or_continue":
            return self._start_or_continue()
        return self._one_shot()

    def _start_or_continue(self) -> str:
        if self.active_session:
            return self.active_session
        self.active_session = self._create_session()
        return self.active_session

    def _one_shot(self) -> str:
        return "no_session"

    def _create_session(self) -> str:
        return "session_001"
