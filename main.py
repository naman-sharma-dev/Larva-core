from core.context import process_user_input


def main() -> None:
    print("Larva (Phase 1) is ready. Type 'help' for commands, 'exit' to quit.")

    while True:
        user_input = input("larva> ").strip()
        response, should_exit = process_user_input(user_input)
        print(response)

        if should_exit:
            break


if __name__ == "__main__":
    main()