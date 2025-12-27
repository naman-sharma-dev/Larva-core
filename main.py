from core.intent_flow import IntentFlow
from core.session_manager import SessionManager
from core.memory import Memory


def main():
    memory = Memory()
    session_manager = SessionManager()
    flow = IntentFlow(memory=memory, session_manager=session_manager)

    print("Larva Core v0")
    print("Type 'exit' to quit\n")

    turn = 1

    while True:
        user_input = input(">> ").strip()

        if not user_input:
            continue

        if user_input.lower() in ["exit", "quit"]:
            print("Session closed.")
            break

        intent = flow.process_input(user_input)
        session = session_manager.handle(intent.session_action)

        memory_touched = bool(intent.entities)

        print(f"\n[Turn {turn}]")
        print(f"Intent Type   : {intent.intent_type}")
        print(f"Entities      : {intent.entities if intent.entities else 'none'}")
        print(f"Session       : {session}")
        print(f"Memory Used   : {'yes' if memory_touched else 'no'}")
        print("-" * 40)

        turn += 1


if __name__ == "__main__":
    main()
