from core.intent_flow import IntentFlow

flow = IntentFlow()
intent = flow.process_input("plan my exam tomorrow")

print(intent)
