from core.intent_engine import IntentEngine
from core.intent_flow import IntentFlow
from core.session_manager import SessionManager
from core.memory import ShortTermMemory
from core.executor import Executor


def main():
    engine = IntentEngine()
    memory = ShortTermMemory()
    flow = IntentFlow(memory)
    sessions = SessionManager()
    executor = Executor()

    active_intent = None

    while True:
        user_input = input("You: ").strip()
        if not user_input:
            continue

        # Clarification path
        if active_intent and active_intent.needs_clarification:
            active_intent = flow.handle_clarification_answer(active_intent, user_input)
        else:
            active_intent = engine.classify(user_input)

        # Process intent (clarification, memory, conflict, etc.)
        active_intent = flow.process(active_intent)

        # Still needs clarification
        if active_intent.needs_clarification:
            print(f"Larva: {active_intent.clarification_prompt}")
            continue

        # Intent resolved → session handling
        session_action = sessions.start_or_continue(active_intent)

        # Write resolved slots to memory
        for slot, data in active_intent.entities.items():
            memory.write(slot, data["value"])

        # 🔹 EXECUTION
        response = executor.execute(active_intent)
        print(f"Larva: {response}")

        print("Intent Resolved")
        print(f"Type    : {active_intent.intent_type}")
        print(f"Entities: {active_intent.entities}")
        print(f"Session : {session_action}")
        print("-" * 40)

        active_intent = None


if __name__ == "__main__":
    main()
