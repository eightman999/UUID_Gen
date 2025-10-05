import Foundation

struct LimitState {
    let isPro: Bool
    let baseLimit: Int
    let bonusActive: Int

    var capacity: Int {
        isPro ? Int.max : baseLimit + bonusActive
    }

    func canAdd(currentCount: Int) -> Bool {
        currentCount < capacity
    }
}

struct LimitConstants {
    static let baseLimit = 10
    static let maxBonus = 5
    static let bonusDuration: TimeInterval = 24 * 60 * 60
}
