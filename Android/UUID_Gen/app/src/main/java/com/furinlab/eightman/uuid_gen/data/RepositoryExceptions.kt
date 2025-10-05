package com.furinlab.eightman.uuid_gen.data

class LimitReachedException : Exception("保存上限に達しました。")
class DuplicateUuidException : Exception("このUUIDは既に保存されています。")
class BonusLimitReachedException : Exception("ボーナス枠の上限に達しています。")
