import Combine
import CoreData
import Foundation
import SwiftUI

struct UuidRecord: Identifiable, Hashable {
    let id: String
    let rawValue: String
    let version: UUIDVersion
    let createdAt: Date
    let label: String?
    let namespace: String?
    let name: String?
    let formatOptions: UuidFormatOptions

    var formattedValue: String {
        format(uuid: UUID(uuidString: rawValue) ?? UUID(), options: formatOptions)
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

struct BonusSlot: Identifiable, Hashable {
    let id: String
    let expiresAt: Date
}

struct EntitlementState {
    var isPro: Bool
    var proSince: Date?
    var lastSyncAt: Date?
}

@MainActor
final class UuidStore: ObservableObject {
    @Published private(set) var items: [UuidRecord] = []
    @Published private(set) var bonusSlots: [BonusSlot] = []
    @Published private(set) var entitlement: EntitlementState
    @Published private(set) var limitState: LimitState

    private let container: NSPersistentContainer
    private var context: NSManagedObjectContext { container.viewContext }

    init(container: NSPersistentContainer = PersistenceController.shared.container) {
        self.container = container
        let entitlement = Self.loadEntitlement(context: container.viewContext)
        self.entitlement = entitlement
        self.limitState = LimitState(isPro: entitlement.isPro, baseLimit: LimitConstants.baseLimit, bonusActive: 0)
        Task {
            await refresh()
        }
    }

    func refresh() async {
        await purgeExpiredBonusSlots()
        let items = await fetchItems()
        let bonus = await fetchBonusSlots()
        await MainActor.run {
            self.items = items
            self.bonusSlots = bonus
            self.limitState = LimitState(isPro: entitlement.isPro, baseLimit: LimitConstants.baseLimit, bonusActive: bonus.count)
        }
    }

    func generateAndSave(version: UUIDVersion, namespace: String?, name: String?, label: String?, formatOptions: UuidFormatOptions) async throws {
        await refresh()
        guard limitState.canAdd(currentCount: items.count) else {
            throw NSError(domain: "UuidStore", code: 1, userInfo: [NSLocalizedDescriptionKey: "保存上限に達しました。"])
        }
        let generated = try UUIDGenerator.generate(version: version, namespaceString: namespace, name: name)
        try await save(generated: generated, label: label, formatOptions: formatOptions)
    }

    func save(generated: GeneratedUuid, label: String?, formatOptions: UuidFormatOptions) async throws {
        let context = context
        try await context.perform {
            let entity = UuidItemEntity(context: context)
            entity.id = generated.id
            entity.value = generated.value.uuidString
            entity.version = generated.version.rawValue
            entity.createdAt = generated.createdAt
            entity.label = label
            entity.namespace = generated.namespace?.uuidString
            entity.name = generated.name
            entity.styleFlags = formatOptions.rawValue
            try context.save()
        }
        await refresh()
    }

    func delete(_ record: UuidRecord) async {
        let context = context
        await context.perform {
            let request: NSFetchRequest<UuidItemEntity> = UuidItemEntity.fetchRequest()
            request.predicate = NSPredicate(format: "id == %@", record.id)
            request.fetchLimit = 1
            if let entity = try? context.fetch(request).first {
                context.delete(entity)
            }
            try? context.save()
        }
        await refresh()
    }

    func removeAll() async {
        let context = context
        await context.perform {
            let fetch: NSFetchRequest<NSFetchRequestResult> = UuidItemEntity.fetchRequest()
            let delete = NSBatchDeleteRequest(fetchRequest: fetch)
            _ = try? context.execute(delete)
            try? context.save()
        }
        await refresh()
    }

    func addBonusSlot() async {
        await purgeExpiredBonusSlots()
        guard bonusSlots.count < LimitConstants.maxBonus else { return }
        let context = context
        await context.perform {
            let entity = BonusSlotEntity(context: context)
            entity.id = UUID().uuidString
            entity.expiresAt = Date().addingTimeInterval(LimitConstants.bonusDuration)
            try? context.save()
        }
        await refresh()
    }

    func purgeExpiredBonusSlots() async {
        let context = context
        await context.perform {
            let request: NSFetchRequest<BonusSlotEntity> = BonusSlotEntity.fetchRequest()
            request.predicate = NSPredicate(format: "expiresAt < %@", Date() as NSDate)
            if let expired = try? context.fetch(request) {
                for entity in expired {
                    context.delete(entity)
                }
                if context.hasChanges {
                    try? context.save()
                }
            }
        }
    }

    func togglePro() async {
        entitlement.isPro.toggle()
        if entitlement.isPro {
            entitlement.proSince = Date()
        } else {
            entitlement.proSince = nil
        }
        await saveEntitlement()
        await refresh()
    }

    private static func loadEntitlement(context: NSManagedObjectContext) -> EntitlementState {
        let request: NSFetchRequest<EntitlementEntity> = EntitlementEntity.fetchRequest()
        request.fetchLimit = 1
        if let entity = try? context.fetch(request).first {
            return EntitlementState(isPro: entity.isPro, proSince: entity.proSince, lastSyncAt: entity.lastSyncAt)
        }
        let newEntity = EntitlementEntity(context: context)
        newEntity.id = "primary"
        newEntity.isPro = false
        newEntity.proSince = nil
        newEntity.lastSyncAt = nil
        try? context.save()
        return EntitlementState(isPro: false, proSince: nil, lastSyncAt: nil)
    }

    private func saveEntitlement() async {
        let context = context
        await context.perform {
            let request: NSFetchRequest<EntitlementEntity> = EntitlementEntity.fetchRequest()
            request.fetchLimit = 1
            let entity = (try? context.fetch(request).first) ?? EntitlementEntity(context: context)
            entity.id = "primary"
            entity.isPro = self.entitlement.isPro
            entity.proSince = self.entitlement.proSince
            entity.lastSyncAt = Date()
            try? context.save()
        }
    }

    private func fetchItems() async -> [UuidRecord] {
        let context = context
        return await context.perform {
            let request: NSFetchRequest<UuidItemEntity> = UuidItemEntity.fetchRequest()
            let sort = NSSortDescriptor(key: "createdAt", ascending: false)
            request.sortDescriptors = [sort]
            let entities = (try? context.fetch(request)) ?? []
            return entities.map { entity in
                UuidRecord(
                    id: entity.id,
                    rawValue: entity.value,
                    version: UUIDVersion(rawValue: entity.version) ?? .v4,
                    createdAt: entity.createdAt,
                    label: entity.label,
                    namespace: entity.namespace,
                    name: entity.name,
                    formatOptions: UuidFormatOptions(rawValue: entity.styleFlags)
                )
            }
        }
    }

    private func fetchBonusSlots() async -> [BonusSlot] {
        let context = context
        return await context.perform {
            let request: NSFetchRequest<BonusSlotEntity> = BonusSlotEntity.fetchRequest()
            let sort = NSSortDescriptor(key: "expiresAt", ascending: true)
            request.sortDescriptors = [sort]
            let entities = (try? context.fetch(request)) ?? []
            return entities.map { entity in
                BonusSlot(id: entity.id, expiresAt: entity.expiresAt)
            }
        }
    }
}

// MARK: - FetchRequest helpers

extension UuidItemEntity {
    @nonobjc class func fetchRequest() -> NSFetchRequest<UuidItemEntity> {
        NSFetchRequest<UuidItemEntity>(entityName: "UuidItemEntity")
    }
}

extension BonusSlotEntity {
    @nonobjc class func fetchRequest() -> NSFetchRequest<BonusSlotEntity> {
        NSFetchRequest<BonusSlotEntity>(entityName: "BonusSlotEntity")
    }
}

extension EntitlementEntity {
    @nonobjc class func fetchRequest() -> NSFetchRequest<EntitlementEntity> {
        NSFetchRequest<EntitlementEntity>(entityName: "EntitlementEntity")
    }
}
