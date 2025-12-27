class Memory:
    def __init__(self):
        self.short_term = {}
        self.long_term = {}

    def get_context(self):
        return self.short_term

    def write_short_term(self, key: str, value):
        self.short_term[key] = value

    def write_long_term(self, key: str, value):
        self.long_term[key] = value

    def read_long_term(self, key: str):
        return self.long_term.get(key)
