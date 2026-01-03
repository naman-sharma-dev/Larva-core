class ShortTermMemory:
    def __init__(self):
        self._data = {}

    def reset(self):
        self._data = {}

    def write(self, key, value):
        self._data[key] = value

    def read(self, key):
        return self._data.get(key)

    def has(self, key):
        return key in self._data

    def get_slot(self, slot_name):
        return self._data.get(slot_name)

    def has_slot(self, slot_name):
        return slot_name in self._data
