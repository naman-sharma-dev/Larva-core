from core.intent_flow import IntentFlow
from core.session_manager import SessionManager
from core.memory import Memory


def main():
    memory = Memory()
    session_manager = SessionManager()
    flow = IntentFlow(memory=memory, session_manager=session_manager)

    user_input = input(">> ")
    intent = flow.process_input(user_input)

    session = session_manager.handle(intent.session_action)

    print("\nIntent:")
    print(intent)

    print("\nSession:")
    print(session)


if __name__ == "__main__":
    main()
